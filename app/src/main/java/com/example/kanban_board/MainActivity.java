package com.example.kanban_board;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserRole = "user";
    private String selectedFilterUser = "Show All Tasks";
    private String currentUserName = ""; // <-- NEW: Added to store the logged-in user's name

    private FloatingActionButton fabAddTask;
    private ViewPager2 viewPager;
    private TaskPagerAdapter pagerAdapter;
    private MenuItem filterMenuItem;
    private ArrayList<String> trackableUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fabAddTask = findViewById(R.id.fabAddTask);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(3);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        pagerAdapter = new TaskPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("📌 TO DO");
            else if (position == 1) tab.setText("⚡ PROGRESS");
            else if (position == 2) tab.setText("✅ DONE");
        }).attach();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            signOutUser();
            return;
        }

        evaluatePermissions(user.getUid());
        fabAddTask.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddTaskActivity.class)));
    }

    public String getCurrentUserRole() { return currentUserRole; }
    public String getSelectedFilterUser() { return selectedFilterUser; }
    public String getCurrentUserName() { return currentUserName; } // <-- NEW: Allows Fragment to ask for the name

    private void evaluatePermissions(String uid) {
        db.collection("Users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    // <-- NEW: Grab the logged-in user's name
                    currentUserName = doc.getString("name");
                    if (currentUserName == null || currentUserName.trim().isEmpty()) {
                        currentUserName = doc.getString("email");
                    }

                    if ("admin".equals(doc.getString("role"))) {
                        currentUserRole = "admin";
                        fabAddTask.setVisibility(View.VISIBLE);
                        if (filterMenuItem != null) filterMenuItem.setVisible(true);
                        invalidateOptionsMenu();
                        loadTeamDirectory();
                    } else {
                        currentUserRole = "user";
                        fabAddTask.setVisibility(View.GONE);
                        if (filterMenuItem != null) filterMenuItem.setVisible(false); // <-- Hides search for users
                        invalidateOptionsMenu();
                    }
                }
                refreshActiveTabs();
            }
        });
    }

    private void loadTeamDirectory() {
        db.collection("Users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                trackableUsers.clear();
                trackableUsers.add("Show All Tasks");
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String displayName = doc.getString("name");
                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = doc.getString("email");
                    }
                    if (displayName != null) {
                        trackableUsers.add(displayName);
                    }
                }
            }
        });
    }

    private void showFilterDialog() {
        String[] options = trackableUsers.toArray(new String[0]);
        int checkedItem = trackableUsers.indexOf(selectedFilterUser);

        new AlertDialog.Builder(this)
                .setTitle("👁️ Choose User to Track")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    selectedFilterUser = options[which];
                    refreshActiveTabs();
                    dialog.dismiss();
                })
                .show();
    }

    private void refreshActiveTabs() {
        for (androidx.fragment.app.Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof TaskColumnFragment) {
                ((TaskColumnFragment) fragment).syncDataRealtime();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        filterMenuItem = menu.findItem(R.id.action_filter);
        if (filterMenuItem != null) {
            // Only show the search bar if they are an admin
            filterMenuItem.setVisible("admin".equals(currentUserRole));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            signOutUser();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOutUser() {
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}