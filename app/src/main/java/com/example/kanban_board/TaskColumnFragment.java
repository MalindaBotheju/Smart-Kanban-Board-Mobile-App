package com.example.kanban_board;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TaskColumnFragment extends Fragment {

    private String columnStatus;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private List<TaskModel> taskList = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration databaseListener;

    public static TaskColumnFragment newInstance(String status) {
        TaskColumnFragment fragment = new TaskColumnFragment();
        Bundle args = new Bundle();
        args.putString("status", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_column, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            columnStatus = getArguments().getString("status");
        }
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        syncDataRealtime();
    }

    public void syncDataRealtime() {
        if (databaseListener != null) databaseListener.remove();

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        String currentRole = activity.getCurrentUserRole();
        String currentFilter = activity.getSelectedFilterUser();

        // Pass user details to the Adapter
        adapter = new TaskAdapter(taskList, currentRole, columnStatus, activity.getCurrentUserName());
        recyclerView.setAdapter(adapter);

        // Optimized Firebase Query
        Query query = db.collection("Tasks").whereEqualTo("status", columnStatus);
        if (!"Show All Tasks".equals(currentFilter)) {
            query = query.whereEqualTo("assignedTo", currentFilter);
        }

        databaseListener = query.addSnapshotListener((value, error) -> {
            if (error != null || value == null || !isAdded()) return;

            taskList.clear(); // Clear old data

            for (QueryDocumentSnapshot doc : value) {
                taskList.add(new TaskModel(
                        doc.getId(),
                        doc.getString("title"),
                        doc.getString("description"),
                        doc.getString("assignedTo")
                ));
            }

            adapter.notifyDataSetChanged(); // Tell RecyclerView to redraw
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (databaseListener != null) databaseListener.remove();
    }

    // --- RECYCLER VIEW ADAPTER ---
    class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
        private List<TaskModel> tasks;
        private String role, status, currentUserName;

        public TaskAdapter(List<TaskModel> tasks, String role, String status, String currentUserName) {
            this.tasks = tasks;
            this.role = role;
            this.status = status;
            this.currentUserName = currentUserName;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_card, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            TaskModel task = tasks.get(position);

            holder.txtContent.setText("📌 " + task.title + "\n📝 " + task.description + "\n👤 Assignee: " + task.assignedTo);

            // Set Column Colors
            if ("TODO".equals(status)) holder.cardBackground.setBackgroundColor(0xFFE0E0E0);
            else if ("PROGRESS".equals(status)) holder.cardBackground.setBackgroundColor(0xFFFFF2CC);
            else if ("DONE".equals(status)) holder.cardBackground.setBackgroundColor(0xFFD5E8D4);

            // Admin Controls Logic
            if ("admin".equals(role)) {
                holder.adminActionLayout.setVisibility(View.VISIBLE);
                holder.btnEdit.setVisibility("DONE".equals(status) ? View.GONE : View.VISIBLE);

                holder.btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(getContext(), AddTaskActivity.class);
                    intent.putExtra("taskId", task.id);
                    intent.putExtra("title", task.title);
                    intent.putExtra("desc", task.description);
                    intent.putExtra("assignee", task.assignedTo);
                    startActivity(intent);
                });

                holder.btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("⚠️ Delete Task")
                            .setMessage("Are you sure you want to permanently delete this task?")
                            .setPositiveButton("Delete", (dialog, which) -> db.collection("Tasks").document(task.id).delete())
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();
                });
            } else {
                holder.adminActionLayout.setVisibility(View.GONE);
            }

            // Moving Tasks Logic
            holder.cardBackground.setOnClickListener(v -> {
                if ("DONE".equals(status)) return;

                if ("admin".equals(role) || (task.assignedTo != null && task.assignedTo.equals(currentUserName))) {
                    String titleText = "TODO".equals(status) ? "🔄 Move to Progress" : "✅ Complete Task";
                    String message = "TODO".equals(status) ? "Do you want to start working on \"" + task.title + "\"?" : "Are you sure you want to mark \"" + task.title + "\" as finished?";
                    String nextStatus = "TODO".equals(status) ? "PROGRESS" : "DONE";

                    new AlertDialog.Builder(getContext())
                            .setTitle(titleText)
                            .setMessage(message)
                            .setPositiveButton("Move", (dialog, which) -> db.collection("Tasks").document(task.id).update("status", nextStatus))
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(getContext(), "✋ You can only move tasks assigned to you!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class TaskViewHolder extends RecyclerView.ViewHolder {
            TextView txtContent, btnEdit, btnDelete;
            LinearLayout cardBackground, adminActionLayout;

            public TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                txtContent = itemView.findViewById(R.id.txtContent);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                cardBackground = itemView.findViewById(R.id.cardBackground);
                adminActionLayout = itemView.findViewById(R.id.adminActionLayout);
            }
        }
    }
}