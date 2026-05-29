package com.example.kanban_board;

public class Task {
    private String id;
    private String title;
    private String description;
    private String status; // Can be "TODO", "PROGRESS", or "DONE"

    // Constructor to build a new task card
    public Task(String id, String title, String description, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // Getters so our Android screens can read the card info
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }

    // Setter to change the status when we move the card to a new column
    public void setStatus(String newStatus) {
        this.status = newStatus;
    }
}