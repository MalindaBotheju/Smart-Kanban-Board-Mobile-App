package com.example.kanban_board;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TaskPagerAdapter extends FragmentStateAdapter {

    public TaskPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        String status = "TODO";
        if (position == 1) status = "PROGRESS";
        else if (position == 2) status = "DONE";

        return TaskColumnFragment.newInstance(status);
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}