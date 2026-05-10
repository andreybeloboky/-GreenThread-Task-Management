// Укажи точный контекстный путь твоего бэкенда
const API_BASE_URL = 'http://localhost:8080/GreenThread-Task-Management';

document.addEventListener('DOMContentLoaded', fetchTasks);

const taskForm = document.getElementById('taskForm');
const taskList = document.getElementById('taskList');
const subtaskForm = document.getElementById('subtaskForm');

// --- Работа с ЗАДАЧАМИ ---

async function fetchTasks() {
    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: 'GET',
            credentials: 'include' // <-- Обязательно для передачи куки сессии
        });

        if (!response.ok) throw new Error(`Ошибка: ${response.status}`);

        const tasks = await response.json();
        renderTasks(tasks);
    } catch (error) {
        console.error('Ошибка при получении задач:', error);
    }
}

taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newTask = {
    title: document.getElementById('taskTitle').value,
            description: document.getElementById('taskDescription').value,
            date: dateValue, // <-- Отправляем как число (например: 1779575040000)
            status: document.getElementById('taskStatus').value
    };

    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: 'POST',
            credentials: 'include', // <-- Включаем credentials
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newTask)
        });

        if (response.ok) {
            taskForm.reset();
            fetchTasks();
        } else {
            const err = await response.json();
            alert(err.message || "Ошибка при создании задачи");
        }
    } catch (error) {
        console.error("Ошибка при отправке:", error);
        alert("Ошибка сети или сервера");
    }
});

async function deleteTask(id) {
    if (!confirm('Удалить эту задачу?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/tasks/${id}`, {
            method: 'DELETE',
            credentials: 'include' // <-- Включаем credentials
        });

        if (!response.ok) {
            const err = await response.json();
            alert(err.message || "Невозможно удалить задачу");
        } else {
            fetchTasks();
        }
    } catch (error) {
        console.error(error);
    }
}

// --- Работа с ПОДЗАДАЧАМИ ---

function openSubtaskModal(taskId) {
    document.getElementById('parentTaskId').value = taskId;
    document.getElementById('subtaskModal').classList.remove('hidden');
}

function closeSubtaskModal() {
    document.getElementById('subtaskModal').classList.add('hidden');
}

subtaskForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const subtask = {
        task_id: parseInt(document.getElementById('parentTaskId').value),
        title: document.getElementById('subtaskTitle').value,
        completed: false
    };

    try {
        const response = await fetch(`${API_BASE_URL}/subtasks`, {
            method: 'POST',
            credentials: 'include', // <-- Включаем credentials
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(subtask)
        });

        if (response.ok) {
            closeSubtaskModal();
            subtaskForm.reset();
            fetchTasks();
        } else {
            const err = await response.json();
            alert(err.message || "Ошибка при создании подзадачи");
        }
    } catch (error) {
        console.error(error);
    }
});

async function toggleSubtask(id, currentStatus, title, taskId) {
    const update = {
        task_id: taskId,
        title: title,
        completed: !currentStatus
    };

    try {
        await fetch(`${API_BASE_URL}/subtasks/${id}`, {
            method: 'PUT',
            credentials: 'include', // <-- Включаем credentials
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(update)
        });
        fetchTasks();
    } catch (error) {
        console.error("Ошибка при обновлении подзадачи:", error);
    }
}

// --- Отрисовка (без изменений) ---

function renderTasks(tasks) {
    taskList.innerHTML = '';

    tasks.forEach(task => {
        const taskCard = document.createElement('div');
        taskCard.className = 'bg-white p-5 rounded-lg shadow-sm border-l-4 ' + getStatusColor(task.status);

        const subtasksHtml = task.subtasks.map(st => `
            <div class="flex items-center justify-between text-sm bg-gray-50 p-2 rounded mt-1">
                <div class="flex items-center">
                    <input type="checkbox" ${st.completed ? 'checked' : ''}
                           onclick="toggleSubtask(${st.id}, ${st.completed}, '${st.title}', ${task.id})"
                           class="mr-2">
                    <span class="${st.completed ? 'line-through text-gray-400' : ''}">${st.title}</span>
                </div>
            </div>
        `).join('');

        taskCard.innerHTML = `
            <div class="flex justify-between items-start">
                <div>
                    <h3 class="font-bold text-lg">${task.title}</h3>
                    <p class="text-gray-600 text-sm">${task.description || 'Нет описания'}</p>
                    <p class="text-xs text-gray-400 mt-1">Дата: ${new Date(task.date).toLocaleString()}</p>
                </div>
                <div class="flex flex-col gap-2">
                    <span class="text-xs font-bold px-2 py-1 rounded bg-gray-200 text-center">${task.status}</span>
                    <button onclick="deleteTask(${task.id})" class="text-red-500 hover:text-red-700 text-sm">Удалить</button>
                </div>
            </div>

            <div class="mt-4">
                <div class="flex justify-between items-center mb-2">
                    <h4 class="text-sm font-semibold text-gray-700">Подзадачи:</h4>
                    <button onclick="openSubtaskModal(${task.id})" class="text-blue-500 text-xs hover:underline">+ Добавить</button>
                </div>
                ${subtasksHtml}
            </div>
        `;
        taskList.appendChild(taskCard);
    });
}

function getStatusColor(status) {
    switch(status) {
        case 'NEW': return 'border-blue-500';
        case 'IN_PROGRESS': return 'border-yellow-500';
        case 'COMPLETED': return 'border-green-500';
        default: return 'border-gray-300';
    }
}