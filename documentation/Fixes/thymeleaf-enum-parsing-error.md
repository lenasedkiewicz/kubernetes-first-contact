# Fix: Thymeleaf Enum Parsing Error

## Error

```
org.springframework.expression.spel.SpelEvaluationException:
EL1007E: Property or field 'name' cannot be found on null
```

The frontend returned HTTP 500 when accessing the Kanban board.

## Root Cause

The Thymeleaf templates were using `.name()` method calls on Java enum values:

```html
<!-- Problematic code in task-card.html -->
th:classappend="${task.priority.name() == 'HIGH' ? 'priority-high' : ...}"
th:if="${task.status.name() != 'TO_DO'}"

<!-- Problematic code in kanban.html -->
th:text="${s.name().replace('_', ' ')}"
```

**Why it failed:**

1. **Thymeleaf parses ALL expressions** during template compilation, even those inside `th:if` blocks that won't be rendered. The expression `${s.name()}` was evaluated even when `isEdit` was false and the block wouldn't render.

2. **Enum serialization issues** - When `TaskDto` is deserialized from JSON via WebClient, the enum fields sometimes caused issues with SpEL's `.name()` method resolution.

## Solution

### 1. Replace `.name()` with `.toString()` and null-safe operators

```html
<!-- Before -->
${task.priority.name() == 'HIGH'}

<!-- After -->
${#strings.toLowerCase(task.priority)}
```

### 2. Use hardcoded options instead of iterating enums

```html
<!-- Before -->
<option th:each="p : ${priorities}" th:value="${p}" th:text="${p}"></option>

<!-- After -->
<option value="LOW">LOW</option>
<option value="MEDIUM">MEDIUM</option>
<option value="HIGH">HIGH</option>
```

### 3. Add null checks for enum comparisons

```html
<!-- Before -->
th:selected="${taskForm.priority == p}"

<!-- After -->
th:selected="${taskForm.priority != null and taskForm.priority.toString() == 'LOW'}"
```

## Key Takeaway

When using Thymeleaf with enums deserialized from JSON (via WebClient/RestTemplate), avoid calling Java-specific methods like `.name()` directly. Use Thymeleaf's built-in utilities like `#strings` or `.toString()` instead.
