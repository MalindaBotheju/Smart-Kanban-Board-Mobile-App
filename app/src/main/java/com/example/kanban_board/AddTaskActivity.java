package com.example.kanban_board;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddTaskActivity extends AppCompatActivity {

    private EditText editNewTitle, editNewDesc;
    private Spinner spinnerNewAssignee;
    private Button btnSubmitTask;
    private TextView txtScreenTitle; // <-- NEW: Added variable for the title text
    private FirebaseFirestore db;
    private ArrayList<String> teamList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private String existingTaskId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        db = FirebaseFirestore.getInstance();

        editNewTitle = findViewById(R.id.editNewTitle);
        editNewDesc = findViewById(R.id.editNewDesc);
        spinnerNewAssignee = findViewById(R.id.spinnerNewAssignee);
        btnSubmitTask = findViewById(R.id.btnSubmitTask);

        // <-- NEW: Link the title TextView (Make sure this ID matches your XML!)
        txtScreenTitle = findViewById(R.id.txtScreenTitle);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, teamList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNewAssignee.setAdapter(adapter);

        // CHECK FOR EDIT MODE
        if (getIntent().hasExtra("taskId")) {
            existingTaskId = getIntent().getStringExtra("taskId");
            editNewTitle.setText(getIntent().getStringExtra("title"));
            editNewDesc.setText(getIntent().getStringExtra("desc"));
            btnSubmitTask.setText("Update Task");

            // <-- NEW: Change the screen title to Update
            if (txtScreenTitle != null) {
                txtScreenTitle.setText("Update Project Task");
            }
        }

        loadTeamMembers();

        btnSubmitTask.setOnClickListener(v -> saveTaskToCloud());
    }

    private void loadTeamMembers() {
        db.collection("Users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                teamList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String displayName = doc.getString("name");

                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = doc.getString("email");
                    }

                    if (displayName != null) {
                        teamList.add(displayName);
                    }
                }
                adapter.notifyDataSetChanged();

                if (existingTaskId != null) {
                    String preSelectedAssignee = getIntent().getStringExtra("assignee");
                    int spinnerPos = adapter.getPosition(preSelectedAssignee);
                    if (spinnerPos != -1) {
                        spinnerNewAssignee.setSelection(spinnerPos);
                    }
                }

            } else {
                Toast.makeText(this, "Failed to load team directory.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTaskToCloud() {
        String title = editNewTitle.getText().toString().trim();
        String desc = editNewDesc.getText().toString().trim();

        if (spinnerNewAssignee.getSelectedItem() == null) {
            Toast.makeText(this, "Assignee missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        String assignee = spinnerNewAssignee.getSelectedItem().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
            Toast.makeText(this, "Fields cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (existingTaskId != null) {
            db.collection("Tasks").document(existingTaskId).update(
                    "title", title,
                    "description", desc,
                    "assignedTo", assignee
            ).addOnSuccessListener(aVoid -> {
                Toast.makeText(AddTaskActivity.this, "Task Updated!", Toast.LENGTH_SHORT).show();
                finish();
            });

        } else {
            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put("title", title);
            taskMap.put("description", desc);
            taskMap.put("status", "TODO");
            taskMap.put("assignedTo", assignee);

            db.collection("Tasks").add(taskMap)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(AddTaskActivity.this, "Published!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }
}