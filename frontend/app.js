// Ensure this matches the context path where your Servlets are deployed
const API_BASE_URL = 'http://localhost:8080/GreenThread-Task-Management';

const taskForm = document.getElementById('taskForm');
const subtaskForm = document.getElementById('subtaskForm');
const taskList = document.getElementById('taskList');
const connectionStatus = document.getElementById('connectionStatus');

// Initialize view
document.addEventListener('DOMContentLoaded', () => {
    setDefaultDate();
    fetchData();
});

// Sets a valid default future date in the datetime-local picker
function setDefaultDate() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    tomorrow.setHours(12, 0, 0, 0);
    const offset = tomorrow.getTimezoneOffset() * 60000;
    const localISO = new Date(tomorrow.getTime() - offset).toISOString().slice(0, 16);
    document.getElementById('taskDate').value = localISO;
}

// Global utility for triggering user feedback toast messages
function showToast(message) {
    document.getElementById('toastMsg').innerText = message;
    const toast = document.getElementById('toastBanner');
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 6000);
}

// Utility to enforce exact Jackson timezone matching pattern: yyyy-MM-dd'T'HH:mm:ss'Z'
function formatToJacksonUTC(datetimeLocalStr) {
    const d = new Date(datetimeLocalStr);
    return d.getUTCFullYear() + '-' +
        String(d.getUTCMonth() + 1).padStart(2, '0') + '-' +
        String(d.getUTCDate()).padStart(2, '0') + 'T' +
        String(d.getUTCHours()).padStart(2, '0') + ':' +
        String(d.getUTCMinutes()).padStart(2, '0') + ':' +
        String(d.getUTCSeconds()).padStart(2, '0') + 'Z';
}

// --- DATA INITIALIZATION ---

// Safely loads tasks and subtasks from independent servlet mappings
async function fetchData() {
    try {
        // Fetch Tasks and Subtasks simultaneously to handle decoupled serialization models safely
        const [tasksRes, subtasksRes] = await Promise.all([
            fetch(`${API_BASE_URL}/tasks`, { credentials: 'include' }),
            fetch(`${API_BASE_URL}/subtasks`, { credentials: 'include' })
        ]);

        if (!tasksRes.ok) throw new Error(`Tasks failed: ${tasksRes.status}`);

        connectionStatus.className = "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200";
        connectionStatus.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Online';

        const rawTasks = await tasksRes.json();
        const rawSubtasks = subtasksRes.ok ? await subtasksRes.json() : [];

        // Consolidate subtask grouping safely via IDs
        const subtaskMap = {};
        rawSubtasks.forEach(st => {
            const parentId = st.taskId || st.task_id; // Map based on varying serializations
            if (!subtaskMap[parentId]) subtaskMap[parentId] = [];
            subtaskMap[parentId].push(st);
        });

        renderTasks(rawTasks, subtaskMap);
    } catch (error) {
        console.error('Data Fetch Error:', error);
        connectionStatus.className = "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200";
        connectionStatus.innerHTML = '<span class="w-1.5 h-1.5 rounded-full bg-rose-500"></span> Disconnected';
        showToast('Unable to reach backend servlets. Check server connection.');
    }
}

// --- TASK MANAGEMENT (POST, PUT, DELETE) ---

taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const titleInput = document.getElementById('taskTitle').value.trim();
    const rawDate = document.getElementById('taskDate').value;

    const payload = {
        id: 0,
        title: titleInput,
        description: document.getElementById('taskDescription').value.trim(),
        date: formatToJacksonUTC(rawDate),
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
            const errMsg = err.error || (err.errors ? JSON.stringify(err.errors) : "Validation failed");
            showToast(`Task creation rejected: ${errMsg}`);
        }
    } catch (error) {
        showToast('Failed to post task over network.');
    }
});

async function updateTaskStatus(taskId, currentStatus, newStatus) {
    if (currentStatus === 'COMPLETED') {
        showToast("Completed tasks cannot change status based on state business rules.");
        return;
    }

    // Retrieve old state properties directly from DOM attributes to build payload cleanly
    const card = document.getElementById(`task-card-${taskId}`);
    const title = card.getAttribute('data-title');
    const desc = card.getAttribute('data-desc');
    const dateStr = card.getAttribute('data-date');

    const payload = {
        id: taskId,
        title: title,
        description: desc,
        date: dateStr,
        status: newStatus
    };

    try {
        // Critical: Passing ID strictly via query parameters (?id=) per your HttpServlet implementation
        const response = await fetch(`${API_BASE_URL}/tasks?id=${taskId}`, {
            method: 'PUT',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showToast(err.error || "Invalid status transition.");
            fetchData(); // Rollback UI selector
        }
    } catch (error) {
        showToast("Failed to communicate update request.");
    }
}

async function deleteTask(taskId) {
    if (!confirm('Are you absolutely certain you want to permanently delete this task?')) return;

    try {
        // Critical: Passing ID strictly via query parameters (?id=) per your HttpServlet implementation
        const response = await fetch(`${API_BASE_URL}/tasks?id=${taskId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.status === 204 || response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            // Likely rejected due to existing subtasks constraint
            showToast(err.error || "Cannot delete task containing subtasks.");
        }
    } catch (error) {
        showToast("Network execution failed during deletion.");
    }
}

// --- SUBTASK MANAGEMENT (POST, PUT, DELETE) ---

function openSubtaskModal(taskId) {
    document.getElementById('parentTaskId').value = taskId;
    document.getElementById('subtaskTitle').value = '';
    document.getElementById('subtaskModal').classList.remove('hidden');
    document.getElementById('subtaskTitle').focus();
}

function closeSubtaskModal() {
    document.getElementById('subtaskModal').classList.add('hidden');
}

subtaskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const parentId = parseInt(document.getElementById('parentTaskId').value);
    const titleVal = document.getElementById('subtaskTitle').value.trim();

    const payload = {
        task_id: parentId,
        title: titleVal,
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
            closeSubtaskModal();
            fetchData();
        } else {
            const err = await response.json();
            showToast(err.error || "Failed to persist subtask.");
        }
    } catch (error) {
        showToast("Subtask assignment failed over connection.");
    }
});

async function toggleSubtask(subtaskId, currentCompleted, title, parentId) {
    const payload = {
        task_id: parentId,
        title: title,
        completed: !currentCompleted
    };

    try {
        // Critical: Passing ID strictly via query parameters (?id=) per your HttpServlet implementation
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
            showToast(err.error || "Unable to update subtask record.");
            fetchData(); // Rollback checkbox visual state
        }
    } catch (error) {
        showToast("Execution aborted while updating subtask.");
    }
}

async function deleteSubtask(subtaskId) {
    try {
        // Critical: Passing ID strictly via query parameters (?id=) per your HttpServlet implementation
        const response = await fetch(`${API_BASE_URL}/subtasks?id=${subtaskId}`, {
            method: 'DELETE',
            credentials: 'include'
        });

        if (response.status === 204 || response.ok) {
            fetchData();
        } else {
            const err = await response.json();
            showToast(err.error || "Could not delete subtask.");
        }
    } catch (error) {
        showToast("Network execution failed during deletion.");
    }
}

// --- DYNAMIC RENDERING ---

function renderTasks(tasks, subtaskMap) {
    taskList.innerHTML = '';

    if (!tasks || tasks.length === 0) {
        taskList.innerHTML = `
            <div class="bg-white p-8 rounded-xl border border-slate-200 text-center text-slate-500">
                <p class="text-sm">No tasks tracked currently. Create one using the form on the left.</p>
            </div>`;
        return;
    }

    tasks.forEach(task => {
        // Handle varying possible fields from backend object mappings safely
        const title = task.title || 'Untitled';
        const desc = task.description || '';
        const status = task.status || 'PENDING';

        // Ensure date formatting parses reliably
        let displayDate = 'No Due Date';
        let rawDateJackson = '';
        if (task.date) {
            rawDateJackson = task.date;
            const d = new Date(task.date);
            displayDate = isNaN(d.getTime()) ? task.date : d.toLocaleString(undefined, {
                month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit'
            });
        }

        // Use internal array OR the dynamically grouped map
        const subtasks = (task.subtasks && task.subtasks.length > 0) ? task.subtasks : (subtaskMap[task.id] || []);

        const cardColor = getStatusTheme(status);

        const card = document.createElement('div');
        card.className = `bg-white rounded-xl border-l-4 ${cardColor.border} border-y border-r border-slate-200 p-5 shadow-sm transition hover:shadow-md`;
        card.id = `task-card-${task.id}`;

        // Cache object properties directly into the element to simplify state reconstruction
        card.setAttribute('data-title', title);
        card.setAttribute('data-desc', desc);
        card.setAttribute('data-date', rawDateJackson);

        // Build status selector dynamically obeying valid transition matrices
        let statusOptionsHtml = '';
        const states = ['PENDING', 'IN_PROGRESS', 'COMPLETED'];
        states.forEach(st => {
            const isSelected = (status === st) ? 'selected' : '';
            // Business rule: COMPLETED states generally lock down changes
            const isDisabled = (status === 'COMPLETED' && st !== 'COMPLETED') ? 'disabled' : '';
            statusOptionsHtml += `<option value="${st}" ${isSelected} ${isDisabled}>${st}</option>`;
        });

        // Generate nested subtask lists securely
        const subtasksHtml = subtasks.map(st => `
            <div class="flex items-center justify-between text-xs bg-slate-50 border border-slate-100 p-2.5 rounded-lg group">
                <label class="flex items-center gap-2 cursor-pointer flex-1 truncate mr-2">
                    <input type="checkbox" ${st.completed ? 'checked' : ''}
                           onchange="toggleSubtask(${st.id}, ${st.completed}, '${st.title.replace(/'/g, "\\'")}', ${task.id})"
                           class="rounded border-slate-300 text-emerald-600 focus:ring-emerald-500 w-4 h-4 cursor-pointer mt-0.5">
                    <span class="truncate font-medium ${st.completed ? 'line-through text-slate-400 font-normal' : 'text-slate-700'}">
                        ${st.title}
                    </span>
                </label>
                <button onclick="deleteSubtask(${st.id})" class="text-slate-400 hover:text-rose-600 opacity-0 group-hover:opacity-100 transition p-1" title="Delete subtask">
                    <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                </button>
            </div>
        `).join('');

        card.innerHTML = `
            <div class="flex justify-between items-start gap-4 mb-3">
                <div class="flex-1 truncate">
                    <h3 class="font-bold text-slate-900 text-base truncate" title="${title}">${title}</h3>
                    ${desc ? `<p class="text-slate-600 text-xs mt-1 break-words line-clamp-2">${desc}</p>` : ''}
                    <div class="flex items-center gap-1.5 text-xs text-slate-400 mt-2 font-mono">
                        <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                        <span>${displayDate}</span>
                    </div>
                </div>

                <div class="flex items-center gap-2 shrink-0">
                    <select onchange="updateTaskStatus(${task.id}, '${status}', this.value)"
                            class="text-xs font-semibold px-2 py-1.5 rounded-lg border border-slate-200 bg-slate-50 ${cardColor.badgeText} focus:outline-none focus:ring-1 focus:ring-slate-300">
                        ${statusOptionsHtml}
                    </select>

                    <button onclick="deleteTask(${task.id})" class="text-slate-400 hover:text-rose-600 transition p-1.5 rounded-lg hover:bg-rose-50" title="Delete Task">
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                    </button>
                </div>
            </div>

            <div class="mt-4 pt-4 border-t border-slate-100">
                <div class="flex justify-between items-center mb-2.5">
                    <span class="text-xs font-semibold text-slate-500 uppercase tracking-wider">Subtasks (${subtasks.length})</span>
                    <button onclick="openSubtaskModal(${task.id})" class="inline-flex items-center gap-1 text-xs font-medium text-emerald-600 hover:text-emerald-700 bg-emerald-50 hover:bg-emerald-100 px-2 py-1 rounded-md transition">
                        <svg class="w-3 h-3" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path></svg>
                        Add
                    </button>
                </div>

                <div class="space-y-1.5">
                    ${subtasksHtml || `<p class="text-xs text-slate-400 italic py-1">No subtasks recorded.</p>`}
                </div>
            </div>
        `;

        taskList.appendChild(card);
    });
}

// Maps appropriate theme decorations based on exact backend state strings
function getStatusTheme(status) {
    switch(status) {
        case 'COMPLETED':   return { border: 'border-emerald-500', badgeText: 'text-emerald-700' };
        case 'IN_PROGRESS': return { border: 'border-amber-500',   badgeText: 'text-amber-700' };
        default:            return { border: 'border-sky-500',     badgeText: 'text-sky-700' };
    }
}