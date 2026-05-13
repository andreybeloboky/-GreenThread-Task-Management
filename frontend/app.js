const API_BASE_URL = 'http://localhost:8080/GreenThread-Task-Management';

const loginView = document.getElementById('loginView');
const dashboardView = document.getElementById('dashboardView');
const connectionStatus = document.getElementById('connectionStatus');
const logoutBtn = document.getElementById('logoutBtn');
const errorAlert = document.getElementById('errorAlert');
const errorAlertText = document.getElementById('errorAlertText');

const loginForm = document.getElementById('loginForm');
const taskForm = document.getElementById('taskForm');

document.addEventListener('DOMContentLoaded', () => {
    setDefaultDate();
    fetchData();
});

function formatToJacksonUTC(datetimeLocalStr) {
    if (!datetimeLocalStr) return null;
    const d = new Date(datetimeLocalStr);
    return d.toISOString().split('.')[0] + 'Z';
}

function setDefaultDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    document.getElementById('taskDate').value = tomorrow.toISOString().slice(0, 16);
}

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
            showError("Неверный логин или пароль");
        }
    } catch (error) {
        showError("Нет связи с сервером");
    }
});

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

        const tasks = await tasksRes.json();
        const subtasks = await subtasksRes.json();

        // Отладочный лог в консоль браузера (F12), чтобы увидеть структуру данных
        console.log("Tasks from server:", tasks);
        console.log("Subtasks from server:", subtasks);

        connectionStatus.innerText = "Online";
        connectionStatus.style.color = "green";

        renderTasks(tasks, subtasks);
    } catch (error) {
        console.error(error);
        connectionStatus.innerText = "Ошибка сети";
        connectionStatus.style.color = "red";
    }
}

function renderTasks(tasks, subtasks) {
    const container = document.getElementById('taskList');
    container.innerHTML = '';

    if (tasks.length === 0) {
        container.innerHTML = '<p>Задач пока нет.</p>';
        return;
    }

    tasks.forEach(task => {
        const taskDiv = document.createElement('div');
        taskDiv.className = 'box';

        // 1. Проверяем, есть ли подзадачи уже внутри объекта Task (из TaskService.java)
        // 2. Если нет, фильтруем внешний массив subtasks, проверяя оба варианта написания ID
        let mySubtasks = task.subtasks || [];

        if (mySubtasks.length === 0 && subtasks) {
            mySubtasks = subtasks.filter(s => {
                const parentId = s.task_id || s.taskId; // Проверка snake_case и camelCase
                return parentId == task.id; // Нестрогое сравнение (==)
            });
        }

        const subtasksHtml = mySubtasks.length > 0
            ? `<h4>Подзадачи:</h4><ul>` + mySubtasks.map(s => `
                <li style="list-style: none; margin-bottom: 5px;">
                    <label style="${s.completed ? 'text-decoration: line-through; color: gray;' : ''}">
                        <input type="checkbox" ${s.completed ? 'checked' : ''}
                               onchange="toggleSubtask(${s.id}, ${s.completed}, '${s.title.replace(/'/g, "\\'")}', ${task.id})">
                        ${s.title}
                    </label>
                    <button onclick="deleteSubtask(${s.id})" style="background:none; color:red; border:none; cursor:pointer; font-size:10px;">[удалить]</button>
                </li>
            `).join('') + `</ul>`
            : `<p><small>Нет подзадач</small></p>`;

        taskDiv.innerHTML = `
            <div style="border-bottom: 1px solid #eee; margin-bottom: 10px; padding-bottom: 5px;">
                <strong style="font-size: 1.2em;">${task.title}</strong>
                <span style="float: right; font-size: 0.8em; padding: 2px 5px; background: #eee; border-radius: 4px;">${task.status}</span>
            </div>
            <p style="color: #666;">${task.description || 'Без описания'}</p>
            <p><small>📅 До: ${new Date(task.date).toLocaleString()}</small></p>

            <div style="background: #fafafa; padding: 10px; border-radius: 4px; margin: 10px 0;">
                ${subtasksHtml}
            </div>

            <button onclick="addSubtask(${task.id})" style="background:#28a745">➕ Подзадача</button>
            <button onclick="deleteTask(${task.id})" style="background:#dc3545">🗑️ Удалить задачу</button>
        `;
        container.appendChild(taskDiv);
    });
}

taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const payload = {
        title: document.getElementById('taskTitle').value,
        description: document.getElementById('taskDescription').value || "",
        date: formatToJacksonUTC(document.getElementById('taskDate').value),
        status: document.getElementById('taskStatus').value
    };

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
    }
});

async function deleteTask(id) {
    if (!confirm("Удалить всю задачу?")) return;
    await fetch(`${API_BASE_URL}/tasks?id=${id}`, { method: 'DELETE', credentials: 'include' });
    fetchData();
}

async function addSubtask(taskId) {
    const title = prompt("Что нужно сделать?");
    if (!title) return;

    await fetch(`${API_BASE_URL}/subtasks`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: taskId, title: title, completed: false })
    });
    fetchData();
}

async function toggleSubtask(id, currentStatus, title, taskId) {
    await fetch(`${API_BASE_URL}/subtasks?id=${id}`, {
        method: 'PUT',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ task_id: taskId, title: title, completed: !currentStatus })
    });
    fetchData();
}

async function deleteSubtask(id) {
    if (!confirm("Удалить подзадачу?")) return;
    await fetch(`${API_BASE_URL}/subtasks?id=${id}`, { method: 'DELETE', credentials: 'include' });
    fetchData();
}

function logout() {
    loginView.classList.remove('hidden');
    dashboardView.classList.add('hidden');
    logoutBtn.classList.add('hidden');
}

function showError(msg) {
    errorAlertText.innerText = msg;
    errorAlert.classList.remove('hidden');
}