package com.example.kanban_board;

public class TaskModel {
    public String id, title, description, assignedTo;

    public TaskModel(String id, String title, String description, String assignedTo) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.assignedTo = assignedTo;
    }
}