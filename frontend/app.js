const API_BASE_URL = 'http://localhost:8080/GreenThread-Task-Management';

const loginView = document.getElementById('loginView');
const dashboardView = document.getElementById('dashboardView');
const connectionStatus = document.getElementById('connectionStatus');
const logoutBtn = document.getElementById('logoutBtn');
const errorAlert = document.getElementById('errorAlert');
const errorAlertText = document.getElementById('errorAlertText');

const loginForm = document.getElementById('loginForm');
const taskForm = document.getElementById('taskForm');
const editTaskForm = document.getElementById('editTaskForm');
const subtaskForm = document.getElementById('subtaskForm');

let tasksDataCache = [];

let editModalInstance = null;
let subtaskModalInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    editModalInstance = new bootstrap.Modal(document.getElementById('editTaskModal'));
    subtaskModalInstance = new bootstrap.Modal(document.getElementById('subtaskModal'));

    setDefaultDate();
    fetchData();
});

function setDefaultDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    const offset = tomorrow.getTimezoneOffset() * 60000;
    const localISO = new Date(tomorrow.getTime() - offset).toISOString().slice(0, 16);
    document.getElementById('taskDate').value = localISO;
}

function formatToJacksonUTC(datetimeLocalStr) {
    const d = new Date(datetimeLocalStr);
    return d.getUTCFullYear() + '-' +
        String(d.getUTCMonth() + 1).padStart(2, '0') + '-' +
        String(d.getUTCDate()).padStart(2, '0') + 'T' +
        String(d.getUTCHours()).padStart(2, '0') + ':' +
        String(d.getUTCMinutes()).padStart(2, '0') + ':' +
        String(d.getUTCSeconds()).padStart(2, '0') + 'Z';
}

function formatToDatetimeLocal(jacksonStr) {
    if (!jacksonStr) return '';
    const d = new Date(jacksonStr);
    if (isNaN(d.getTime())) return '';
    const offset = d.getTimezoneOffset() * 60000;
    return new Date(d.getTime() - offset).toISOString().slice(0, 16);
}

function showBackendError(responseStatus, errObj) {
    let message = "Invalid login/password";

    if (errObj.error) {
        try {
            const nested = JSON.parse(errObj.error);
            if (nested.errors) {
                message = Object.entries(nested.errors)
                    .map(([field, msg]) => `• ${msg}`)
                    .join('<br>');
            } else {
                message = errObj.error;
            }
        } catch(e) {
            if (errObj.error.includes("already created")) {
                message = "A task with that name already exists! Choose a unique name.";
            } else if (errObj.error.includes("includes subtasks")) {
                message = "An issue containing active subtasks cannot be deleted.";
            } else {
                message = errObj.error;
            }
        }
    } else if (errObj.errors) {
        message = Object.values(errObj.errors).join('<br>');
    } else if (errObj.message) {
        message = errObj.message;
    }

    errorAlertText.innerHTML = message;
    errorAlert.classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorAlert.classList.add('d-none');

    const credentials = {
        username: document.getElementById('loginUser').value.trim(),
        password: document.getElementById('loginPass').value.trim()
    };

    try {
        const response = await fetch(`${API_BASE_URL}/login`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(credentials)
        });

        if (response.ok) {
            loginView.classList.add('d-none');
            dashboardView.classList.remove('d-none');
            logoutBtn.classList.remove('d-none');
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "Error connecting to the authorization server.";
        errorAlert.classList.remove('d-none');
    }
});

function logout() {
    loginView.classList.remove('d-none');
    dashboardView.classList.add('d-none');
    logoutBtn.classList.add('d-none');
    connectionStatus.className = "badge bg-warning text-dark";
    connectionStatus.innerText = "Entrance is required";
}

async function fetchData() {
    errorAlert.classList.add('d-none');
    try {
        const [tasksRes, subtasksRes] = await Promise.all([
            fetch(`${API_BASE_URL}/tasks`, { credentials: 'include' }),
            fetch(`${API_BASE_URL}/subtasks`, { credentials: 'include' })
        ]);

        if (tasksRes.status === 401 || subtasksRes.status === 401) {
            logout();
            return;
        }

        if (!tasksRes.ok) throw new Error("Failed to load tasks");

        connectionStatus.className = "badge bg-success";
        connectionStatus.innerText = "Online";
        loginView.classList.add('d-none');
        dashboardView.classList.remove('d-none');
        logoutBtn.classList.remove('d-none');

        const rawTasks = await tasksRes.json();
        const rawSubtasks = subtasksRes.ok ? await subtasksRes.json() : [];

        tasksDataCache = rawTasks;

        const subtaskMap = {};
        rawSubtasks.forEach(st => {
            const parentId = st.taskId || st.task_id;
            if (!subtaskMap[parentId]) subtaskMap[parentId] = [];
            subtaskMap[parentId].push(st);
        });

        renderTasks(rawTasks, subtaskMap);
    } catch (error) {
        connectionStatus.className = "badge bg-danger";
        connectionStatus.innerText = "Network error";
    }
}

taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorAlert.classList.add('d-none');

    const payload = {
        id: 0,
        title: document.getElementById('taskTitle').value.trim(),
        description: document.getElementById('taskDescription').value.trim(),
        date: formatToJacksonUTC(document.getElementById('taskDate').value),
        status: document.getElementById('taskStatus').value
    };

    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            taskForm.reset();
            setDefaultDate();
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "The issue data could not be sent.";
        errorAlert.classList.remove('d-none');
    }
});

function openEditModal(taskId) {
    errorAlert.classList.add('d-none');
    const task = tasksDataCache.find(t => t.id === taskId);
    if (!task) return;

    document.getElementById('editTaskId').value = task.id;
    document.getElementById('editTaskTitle').value = task.title || '';
    document.getElementById('editTaskDescription').value = task.description || '';
    document.getElementById('editTaskDate').value = formatToDatetimeLocal(task.date);

    const statusSelect = document.getElementById('editTaskStatus');
    statusSelect.value = task.status || 'PENDING';

    Array.from(statusSelect.options).forEach(option => {
        option.disabled = false;
        if (task.status === 'COMPLETED' && option.value !== 'COMPLETED') {
            option.disabled = true;
        } else if (task.status === 'PENDING' && option.value === 'COMPLETED') {
            option.disabled = true;
        }
    });

    editModalInstance.show();
}

editTaskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorAlert.classList.add('d-none');

    const taskId = parseInt(document.getElementById('editTaskId').value);

    const payload = {
        id: taskId,
        title: document.getElementById('editTaskTitle').value.trim(),
        description: document.getElementById('editTaskDescription').value.trim(),
        date: formatToJacksonUTC(document.getElementById('editTaskDate').value),
        status: document.getElementById('editTaskStatus').value
    };

    try {
        const response = await fetch(`${API_BASE_URL}/tasks?id=${taskId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            editModalInstance.hide();
            fetchData();
        } else {
            const err = await response.json();
            editModalInstance.hide();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "Couldn't save the changes.";
        errorAlert.classList.remove('d-none');
    }
});

async function deleteTask(taskId) {
    if (!confirm("Are you sure you want to delete this task?")) return;
    errorAlert.classList.add('d-none');

    try {
        const response = await fetch(`${API_BASE_URL}/tasks?id=${taskId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.status === 204 || response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "An error occurred when deleting an issue.";
        errorAlert.classList.remove('d-none');
    }
}

function openSubtaskModal(taskId) {
    errorAlert.classList.add('d-none');
    document.getElementById('parentTaskId').value = taskId;
    document.getElementById('subtaskTitle').value = '';
    subtaskModalInstance.show();
}

subtaskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorAlert.classList.add('d-none');

    const parentId = parseInt(document.getElementById('parentTaskId').value);
    const payload = {
        task_id: parentId,
        title: document.getElementById('subtaskTitle').value.trim(),
        completed: false
    };

    try {
        const response = await fetch(`${API_BASE_URL}/subtasks`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            subtaskModalInstance.hide();
            fetchData();
        } else {
            const err = await response.json();
            subtaskModalInstance.hide();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "Couldn't add a subtask.";
        errorAlert.classList.remove('d-none');
    }
});

async function toggleSubtask(subtaskId, currentCompleted, title, parentId) {
    errorAlert.classList.add('d-none');
    const payload = {
        task_id: parentId,
        title: title,
        completed: !currentCompleted
    };

    try {
        const response = await fetch(`${API_BASE_URL}/subtasks?id=${subtaskId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
            fetchData();
        }
    } catch (error) {
        errorAlertText.innerText = "Error updating the status of a subtask.";
        errorAlert.classList.remove('d-none');
    }
}

async function deleteSubtask(subtaskId) {
    errorAlert.classList.add('d-none');
    try {
        const response = await fetch(`${API_BASE_URL}/subtasks?id=${subtaskId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.status === 204 || response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "Error when deleting a subtask.";
        errorAlert.classList.remove('d-none');
    }
}

function renderTasks(tasks, subtaskMap) {
    const taskList = document.getElementById('taskList');
    taskList.innerHTML = '';

    if (!tasks || tasks.length === 0) {
        taskList.innerHTML = `<div class="alert alert-secondary text-center">The task list is empty. Create a new task.</div>`;
        return;
    }

    tasks.forEach((task, index) => {
        const title = task.title || 'Untitled';
        const desc = task.description || '';
        const status = task.status || 'PENDING';

        let displayDate = 'There is no date';
        if (task.date) {
            const d = new Date(task.date);
            displayDate = isNaN(d.getTime()) ? task.date : d.toLocaleString();
        }

        const subtasks = (task.subtasks && task.subtasks.length > 0) ? task.subtasks : (subtaskMap[task.id] || []);

        let badgeClass = 'bg-secondary';
        if (status === 'COMPLETED') badgeClass = 'bg-success';
        if (status === 'IN_PROGRESS') badgeClass = 'bg-warning text-dark';

        const subtasksHtml = subtasks.map(st => `
            <li class="list-group-item d-flex justify-content-between align-items-center bg-light p-2">
                <div class="form-check mb-0 flex-grow-1">
                    <input class="form-check-input" type="checkbox" id="st-${st.id}" ${st.completed ? 'checked' : ''}
                           onchange="toggleSubtask(${st.id}, ${st.completed}, '${st.title.replace(/'/g, "\\'")}', ${task.id})">
                    <label class="form-check-label ${st.completed ? 'text-decoration-line-through text-muted' : ''}" for="st-${st.id}">
                        ${st.title}
                    </label>
                </div>
                <div class="btn-group">
                    <button onclick="editSubtaskTitle(${st.id}, '${st.title.replace(/'/g, "\\'")}', ${st.completed}, ${task.id})"
                            class="btn btn-sm btn-link text-primary p-0 me-2" title="Edit">
                        <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10"></path>
                        </svg>
                    </button>
                    <button onclick="deleteSubtask(${st.id})" class="btn btn-sm btn-link text-danger p-0" title="Delete">&times;</button>
                </div>
            </li>
        `).join('');

        const accordionId = `collapseTask${task.id}`;

        const item = document.createElement('div');
        item.className = 'accordion-item mb-2 border rounded';
        item.innerHTML = `
            <h2 class="accordion-header" id="heading${task.id}">
                <button class="accordion-button ${index !== 0 ? 'collapsed' : ''}" type="button" data-bs-toggle="collapse" data-bs-target="#${accordionId}">
                    <div class="d-flex justify-content-between items-center w-100 me-3">
                        <span class="font-weight-bold">${title}</span>
                        <div>
                            <span class="badge ${badgeClass} me-2">${status}</span>
                            <span class="badge bg-light text-dark border"><small>${displayDate}</small></span>
                        </div>
                    </div>
                </button>
            </h2>

            <div id="${accordionId}" class="accordion-collapse collapse ${index === 0 ? 'show' : ''}" data-bs-parent="#taskList">
                <div class="accordion-body">
                    ${desc ? `<p class="mb-3 text-muted">${desc}</p>` : ''}

                    <div class="d-flex gap-2 mb-3">
                        <button onclick="openEditModal(${task.id})" class="btn btn-primary btn-sm">Edit task</button>
                        <button onclick="deleteTask(${task.id})" class="btn btn-danger btn-sm">Delete task</button>
                        <button onclick="openSubtaskModal(${task.id})" class="btn btn-success btn-sm ms-auto">+ Add subtask</button>
                    </div>

                    <h6 class="text-muted mt-2">Subtasks (${subtasks.length}):</h6>
                    <ul class="list-group list-group-flush border rounded">
                        ${subtasksHtml || `<li class="list-group-item text-muted text-center"><small>There are no attached subtasks</small></li>`}
                    </ul>
                </div>
            </div>
        `;
        taskList.appendChild(item);
    });
}

async function editSubtaskTitle(subtaskId, currentTitle, isCompleted, parentId) {
    errorAlert.classList.add('d-none');

    const newTitle = prompt("Enter a new subtask name (minimum 5 characters):", currentTitle);

    if (newTitle === null || newTitle.trim() === currentTitle) return;

    const cleanedTitle = newTitle.trim();

    if (cleanedTitle.length < 5) {
        errorAlertText.innerText = "Error: The name of the subtask must contain at least 5 characters.";
        errorAlert.classList.remove('d-none');
        window.scrollTo({ top: 0, behavior: 'smooth' });
        return;
    }

    const payload = {
        task_id: parentId,
        title: cleanedTitle,
        completed: isCompleted
    };

    try {
        const response = await fetch(`${API_BASE_URL}/subtasks?id=${subtaskId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showBackendError(response.status, err);
        }
    } catch (error) {
        errorAlertText.innerText = "Network error when trying to update a subtask.";
        errorAlert.classList.remove('d-none');
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}