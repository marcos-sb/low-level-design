// Task Creation: Users can create tasks with a title, description, priority, and assignee.
// Task Assignment: Tasks can be assigned to users and reassigned as needed.
// Task Status: Tasks can have statuses such as TODO, IN_PROGRESS, DONE, etc.
// Task Priority: Tasks can have priorities such as LOW, MEDIUM, HIGH.
// Comments: Users can add comments to tasks.
// Task Updates: Tasks can be updated (status, priority, assignee, etc.).
// Task Listing: List all tasks, or filter by status, priority, or assignee.
// Extensibility: Easy to add new statuses, priorities, or features.

public record Task {
    final long id;
    final String title;
    final String description;
    Priority priority;
    User assignee;
    Status status;
    final List<Comment> comments;
}

public record User {
    final String username;
    final String name;
}

public class TaskManager {
    final Map<Long, Task> tasks;
    public void createTask(String title, String description) {}
    public void assignTask(Task task, User user) {}
    public void setStatus(Task task, Status status) {}
    public void setPriority(Task task, Priority priority) {}
    public List<Task> listTasks(TaskFilter filter) {}
}

public enum Status {
    TODO, IN_PROGRESS, DONE
}

public enum Priority {
    LOW, MEDIUM, HIGH
}

public record Comment {
    final String text;
}

public interface TaskFilter implements Predicate<Task> {}

public class StatusFilter implements TaskFilter {
    final Status status;
    @Override
    public boolean test(Task task) {
        return status.equals(task.getStatus());
    }
}

public class PriorityFilter implements TaskFilter {
    final Set<Priority> priorities;
    @Override
    public boolean test(Task task) {
        return priorities.contains(task.getPriority());
    }
}

public class AndFilter implements TaskFilter {
    final TaskFilter[] filters;
    public AndFilter(TaskFilter... filters) {
        this.filters = filters;
    }
    @Override
    public boolean test(Task task) {
        for (var filter : filters) {
            if (!filter.test(task)) {
                return false;
            }
        }
        return true;
    }
}
