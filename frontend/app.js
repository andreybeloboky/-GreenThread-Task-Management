const API_BASE_URL = 'http://localhost:8080/GreenThread-Task-Management';

const loginView = document.getElementById('loginView');
const dashboardView = document.getElementById('dashboardView');
const connectionStatus = document.getElementById('connectionStatus');
const logoutBtn = document.getElementById('logoutBtn');
const errorAlert = document.getElementById('errorAlert');
const errorAlertText = document.getElementById('errorAlertText');

const loginForm = document.getElementById('loginForm');
const taskForm = document.getElementById('taskForm');
const editModal = document.getElementById('editTaskModal');
const editForm = document.getElementById('editTaskForm');

let globalTasks = [];

document.addEventListener('DOMContentLoaded', () => {
    setDefaultDate();
    fetchData();
});

// Форматирование даты для сервера
function formatToJacksonUTC(datetimeLocalStr) {
    if (!datetimeLocalStr) return null;
    const d = new Date(datetimeLocalStr);
    return d.toISOString().split('.')[0] + 'Z';
}

// Форматирование даты для модального окна
function formatToDatetimeLocal(jacksonStr) {
    if (!jacksonStr) return '';
    const date = new Date(jacksonStr);
    const localDate = new Date(date.getTime() - (date.getTimezoneOffset() * 60000));
    return localDate.toISOString().slice(0, 16);
}

function setDefaultDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    document.getElementById('taskDate').value = tomorrow.toISOString().slice(0, 16);
}

// 2. ЛОГИН
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    errorAlert.classList.add('hidden');

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
            loginView.classList.add('hidden');
            dashboardView.classList.remove('hidden');
            logoutBtn.classList.remove('hidden');
            fetchData();
        } else {
            const errData = await response.json().catch(() => ({}));
            showError(errData.message || "Invalid login/password");
        }
    } catch (error) {
        showError("There is no connection to the server.");
    }
});

// 3. ЗАГРУЗКА ДАННЫХ
async function fetchData() {
    try {
        const [tasksRes, subtasksRes] = await Promise.all([
            fetch(`${API_BASE_URL}/tasks`, { credentials: 'include' }),
            fetch(`${API_BASE_URL}/subtasks`, { credentials: 'include' })
        ]);

        if (tasksRes.status === 401) {
            logout();
            return;
        }

        if (tasksRes.ok && subtasksRes.ok) {
            const tasks = await tasksRes.json();
            const subtasks = await subtasksRes.json();

            // Сортируем основные задачи по ID
            tasks.sort((a, b) => a.id - b.id);
            globalTasks = tasks;

            connectionStatus.innerText = "Online";
            connectionStatus.style.color = "green";
            renderTasks(tasks, subtasks);
        }
    } catch (error) {
        connectionStatus.innerText = "Network error";
        connectionStatus.style.color = "red";
    }
}

// 4. ОТОБРАЖЕНИЕ (С ИСПРАВЛЕННОЙ СОРТИРОВКОЙ)
function renderTasks(tasks, subtasks) {
    const container = document.getElementById('taskList');
    container.innerHTML = '';

    tasks.forEach(task => {
        const taskDiv = document.createElement('div');
        taskDiv.className = 'box';

        // 1. Получаем и сортируем подзадачи
        let mySubtasks = subtasks.filter(s => (s.task_id || s.taskId) == task.id);
        mySubtasks.sort((a, b) => a.id - b.id);

        // 2. Формируем HTML для списка подзадач (только если они есть)
        let subtasksSection = ""; // По умолчанию пусто

        if (mySubtasks.length > 0) {
            const itemsHtml = mySubtasks.map(s => `
                <li style="list-style: none; display: flex; align-items: center; justify-content: space-between; padding: 5px 0;">
                    <label style="cursor: pointer; flex-grow: 1; ${s.completed ? 'text-decoration: line-through; color: gray;' : ''}">
                        <input type="checkbox" ${s.completed ? 'checked' : ''}
                               onchange="toggleSubtask(${s.id}, ${s.completed}, '${s.title.replace(/'/g, "\\'")}', ${task.id})"
                               style="width: auto; display: inline; margin-right: 10px;">
                        ${s.title}
                    </label>
                    <div style="display: flex; gap: 10px;">
                        <button onclick="editSubtask(${s.id}, ${task.id}, ${s.completed}, '${s.title.replace(/'/g, "\\'")}')"
                                style="background:none; border:none; padding:0; cursor:pointer; font-size:1.2em;">✏️</button>
                        <button onclick="deleteSubtask(${s.id})"
                                style="background:none; border:none; padding:0; cursor:pointer; font-size:1.2em;">🗑️</button>
                    </div>
                </li>`).join('');

            // Если есть хотя бы одна подзадача, создаем "квадрат" с фоном
            subtasksSection = `
                <div style="background: #f9f9f9; padding: 10px; border-radius: 5px; margin: 10px 0; border: 1px solid #eee;">
                    <h4 style="margin: 0 0 10px 0; font-size: 0.9em; color: #666;">Subtasks:</h4>
                    <ul style="padding: 0; margin: 0;">${itemsHtml}</ul>
                </div>`;
        }

        // 3. Собираем итоговую карточку задачи
        taskDiv.innerHTML = `
            <div style="border-bottom: 1px solid #eee; margin-bottom: 10px; padding-bottom: 5px;">
                <strong style="font-size: 1.1em;">${task.title}</strong>
                <span style="float: right; font-size: 0.8em; color: #777;">${task.status}</span>
            </div>
            <p style="font-size: 0.9em; color: #555; margin-bottom: 10px;">${task.description || '<i></i>'}</p>
            <p style="margin-bottom: 10px;"><small>📅 Up to: ${new Date(task.date).toLocaleString()}</small></p>

            ${subtasksSection} <div style="display: flex; gap: 5px; margin-top: 10px;">
                <button onclick="addSubtask(${task.id})" style="background:#28a745; font-size: 0.8em;">➕ Add subtask</button>
                <button onclick="openEditModal(${task.id})" style="background:#ffc107; color:black; font-size: 0.8em;">✏️ Edit</button>
                <button onclick="deleteTask(${task.id})" style="background:#dc3545; font-size: 0.8em;">🗑️ DELETE</button>
            </div>
        `;
        container.appendChild(taskDiv);
    });
}

// Функции модального окна
function openEditModal(id) {
    const task = globalTasks.find(t => t.id == id);
    if (!task) return;
    document.getElementById('editTaskId').value = task.id;
    document.getElementById('editTaskTitle').value = task.title;
    document.getElementById('editTaskDescription').value = task.description || '';
    document.getElementById('editTaskDate').value = formatToDatetimeLocal(task.date);
    document.getElementById('editTaskStatus').value = task.status;
    editModal.classList.remove('hidden');
}

function closeEditModal() {
    editModal.classList.add('hidden');
}

editForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('editTaskId').value;
    const payload = {
        title: document.getElementById('editTaskTitle').value,
        description: document.getElementById('editTaskDescription').value,
        date: formatToJacksonUTC(document.getElementById('editTaskDate').value),
        status: document.getElementById('editTaskStatus').value
    };
    await fetch(`${API_BASE_URL}/tasks?id=${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    closeEditModal();
    fetchData();
});

// Действия с подзадачами
async function toggleSubtask(id, currentStatus, title, taskId) {
    await fetch(`${API_BASE_URL}/subtasks?id=${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: taskId, title: title, completed: !currentStatus })
    });
    fetchData();
}

async function addSubtask(taskId) {
    const title = prompt("What needs to be done?");
    if (!title) return;
    await fetch(`${API_BASE_URL}/subtasks`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: taskId, title: title, completed: false })
    });
    fetchData();
}

async function editSubtask(id, taskId, completed, currentTitle) {
    const newTitle = prompt("New subtask title:", currentTitle);
    if (!newTitle) return;
    await fetch(`${API_BASE_URL}/subtasks?id=${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: taskId, title: newTitle, completed: completed })
    });
    fetchData();
}

async function deleteSubtask(id) {
    if (!confirm("Delete?")) return;
    await fetch(`${API_BASE_URL}/subtasks?id=${id}`, { method: 'DELETE', credentials: 'include' });
    fetchData();
}

async function deleteTask(id) {
    if (!confirm("Delete whole task?")) return;
    await fetch(`${API_BASE_URL}/tasks?id=${id}`, { method: 'DELETE', credentials: 'include' });
    fetchData();
}

taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        title: document.getElementById('taskTitle').value,
        description: document.getElementById('taskDescription').value || "",
        date: formatToJacksonUTC(document.getElementById('taskDate').value),
        status: document.getElementById('taskStatus').value
    };
    await fetch(`${API_BASE_URL}/tasks`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    taskForm.reset();
    setDefaultDate();
    fetchData();
});

function logout() {
    loginView.classList.remove('hidden');
    dashboardView.classList.add('hidden');
    logoutBtn.classList.add('hidden');
}

function showError(msg) {
    errorAlertText.innerText = msg;
    errorAlert.classList.remove('hidden');
}