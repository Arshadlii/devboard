
const API = "/api";

const $ = (id) => document.getElementById(id);

let currentUser = null;
let projects = [];
let selectedProject = null;
let selectedTask = null;
let authMode = "login";
let toastTimer;

const statuses = [
    { value: "TODO", label: "To do" },
    { value: "IN_PROGRESS", label: "In progress" },
    { value: "IN_REVIEW", label: "In review" },
    { value: "COMPLETED", label: "Completed" }
];

const statusLabels = Object.fromEntries(
    statuses.map((item) => [item.value, item.label])
);

function idOf(item) {
    return item?.id ?? item?.projectId ?? item?.taskId ?? item?.userId;
}

function showToast(message, error = false) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.remove("hidden", "error");

    if (error) toast.classList.add("error");

    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        toast.classList.add("hidden");
    }, 4000);
}

async function api(path, options = {}) {
    const config = {
        ...options,
        credentials: "same-origin",
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...options.headers
        }
    };

    let response;

    try {
        response = await fetch(API + path, config);
    } catch {
        throw new Error(
            "Cannot reach the server. Check that Spring Boot is running."
        );
    }

    if (response.status === 204) return null;

    const text = await response.text();
    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (!response.ok) {
        const message =
            typeof data === "string"
                ? data
                : data?.detail ||
                  data?.message ||
                  data?.error ||
                  data?.title ||
                  `Request failed (${response.status})`;

        if (response.status === 401) {
            showAuth();
        }

        throw new Error(message);
    }

    return data;
}

function showAuth() {
    $("authScreen").classList.remove("hidden");
    $("appScreen").classList.add("hidden");
    currentUser = null;
}


function showApp() {
    $("authScreen").classList.add("hidden");
    $("appScreen").classList.remove("hidden");

    const name = currentUser?.name || currentUser?.email || "User";

    $("userName").textContent = name;

    const role = currentUser?.role
        || currentUser?.roles?.find(r =>
            r.authority?.startsWith("ROLE_")
        )?.authority?.replace(/^ROLE_/, "")
        || "USER";

    $("userRole").textContent = role;
    $("userAvatar").textContent = name.charAt(0).toUpperCase();

    const canCreateProject = ["ADMIN", "PROJECT_MANAGER"]
        .includes(role);

    $("newProjectBtn").classList.toggle(
        "hidden", !canCreateProject
    );
}


function getCurrentRole() {
    return currentUser?.role
        || currentUser?.roles?.find(r =>
            r.authority?.startsWith("ROLE_")
        )?.authority?.replace(/^ROLE_/, "")
        || "USER";
}

function updateProjectPermissions() {
    const role = getCurrentRole();

    const isAdmin = role === "ADMIN";
    const isProjectManager = role === "PROJECT_MANAGER";

    // The backend must still enforce these permissions.
    const canManageProject = isAdmin || isProjectManager;

    $("newTaskBtn").classList.toggle(
        "hidden", !canManageProject
    );

    $("addMemberBtn").classList.toggle(
        "hidden", !canManageProject
    );

    $("newProjectBtn").classList.toggle(
        "hidden", !canManageProject
    );
}





function setAuthMode(mode) {
    authMode = mode;
    const registering = mode === "register";

    $("authTitle").textContent =
        registering ? "Create your account" : "Welcome back";

    $("authSubtitle").textContent =
        registering
            ? "Register to start using DevBoard."
            : "Sign in to continue to your workspace.";

    $("nameGroup").classList.toggle("hidden", !registering);
    $("authName").required = registering;
    $("authPassword").autocomplete =
        registering ? "new-password" : "current-password";

    $("authSubmit").textContent =
        registering ? "Create account" : "Sign in";

    $("authSwitchText").textContent =
        registering ? "Already have an account?" : "New to DevBoard?";

    $("authSwitch").textContent =
        registering ? "Sign in" : "Create account";
}

$("authSwitch").addEventListener("click", () => {
    setAuthMode(authMode === "login" ? "register" : "login");
});

$("authForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = $("authSubmit");
    button.disabled = true;

    try {
        const email = $("authEmail").value.trim();
        const password = $("authPassword").value;

        if (authMode === "register") {
            await api("/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    name: $("authName").value.trim(),
                    email,
                    password
                })
            });

            showToast("Account created. Please sign in.");
            setAuthMode("login");
            $("authPassword").value = "";
            return;
        }

        await api("/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        });

        await loadCurrentUser();
        showToast("Welcome to DevBoard!");
    } catch (error) {
        showToast(error.message, true);
    } finally {
        button.disabled = false;
    }
});

async function loadCurrentUser() {
    currentUser = await api("/auth/me");
    showApp();
    updateProjectPermissions();
    await loadProjects();
}

$("logoutBtn").addEventListener("click", async () => {
    try {
        await api("/auth/logout", { method: "POST" });
    } catch (error) {
        showToast(error.message, true);
    } finally {
        showAuth();
        selectedProject = null;
        selectedTask = null;
        projects = [];
    }
});

function openDialog(id) {
    $(id).showModal();
}

document.querySelectorAll(".close-dialog").forEach((button) => {
    button.addEventListener("click", () => {
        button.closest("dialog").close();
    });
});

function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, (character) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);
}

function formatDate(value) {
    if (!value) return "No deadline";

    const date = new Date(value + "T00:00:00");

    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleDateString(undefined, {
        day: "numeric",
        month: "short",
        year: "numeric"
    });
}

function showEmpty(container, message) {
    container.innerHTML =
        `<div class="empty-state">${escapeHTML(message)}</div>`;
}

function normalizeArray(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.items)) return data.items;
    return [];
}

async function loadProjects() {
    try {
        const data = await api("/projects");
        projects = normalizeArray(data);
        renderProjects();
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderProjects() {
    $("projectCount").textContent = projects.length;

    const grid = $("projectsGrid");

    if (!projects.length) {
        showEmpty(grid, "No projects found. Create a project to get started.");
        return;
    }

    grid.innerHTML = projects.map((project, index) => {
        const projectId = idOf(project);
        const name = project.name || "Untitled project";
        const description = project.description || "No description provided";
        const status = project.status || "PLANNING";

        return `
            <article class="project-card" data-project-index="${index}">
                <div class="project-card-footer">
                    <span>PROJECT</span>
                    <span class="pill">${escapeHTML(status.replaceAll("_", " "))}</span>
                </div>
                <h3>${escapeHTML(name)}</h3>
                <p>${escapeHTML(description)}</p>
                <div class="project-card-footer">
                    <span>ID: ${escapeHTML(projectId ?? "—")}</span>
                    <span>Open project →</span>
                </div>
            </article>
        `;
    }).join("");

    grid.querySelectorAll("[data-project-index]").forEach((card) => {
        card.addEventListener("click", () => {
            const project = projects[Number(card.dataset.projectIndex)];
            openProject(project);
            updateProjectPermissions();
        });
    });
}


$("newProjectBtn").addEventListener("click", () => {
    const role = currentUser?.role
        || currentUser?.roles?.find(r =>
            r.authority?.startsWith("ROLE_")
        )?.authority?.replace(/^ROLE_/, "");

    if (!["ADMIN", "PROJECT_MANAGER"].includes(role)) {
        showToast(
            "Only project managers or administrators can create projects.",
            true
        );
        return;
    }

    $("projectForm").reset();
    openDialog("projectDialog");
});


$("projectForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = event.submitter;
    if (button) button.disabled = true;

    const body = {
        name: $("projectName").value.trim(),
        description: $("projectDescription").value.trim(),
        priority: $("projectPriority").value
    };

    const deadline = $("projectDeadline").value;
    if (deadline) body.deadline = deadline;

    try {
        const project = await api("/projects", {
            method: "POST",
            body: JSON.stringify(body)
        });

        $("projectDialog").close();
        showToast("Project created successfully.");

        await loadProjects();

        if (project && idOf(project) != null) {
            const savedProject = projects.find(
                (item) => String(idOf(item)) === String(idOf(project))
            );
            if (savedProject) await openProject(savedProject);
        }
    } catch (error) {
        showToast(error.message, true);
    } finally {
        if (button) button.disabled = false;
    }
});

async function openProject(project) {
    selectedProject = project;

    $("dashboardView").classList.add("hidden");
    $("projectView").classList.remove("hidden");
    $("pageTitle").textContent = project.name || "Project";

    $("selectedProjectName").textContent =
        project.name || "Untitled project";

    $("selectedProjectDescription").textContent =
        project.description || "No description provided.";

    await Promise.all([
    loadTasks(),
    loadMembers()
]);
}

$("backBtn").addEventListener("click", () => {
    selectedProject = null;
    selectedTask = null;

    $("projectView").classList.add("hidden");
    $("dashboardView").classList.remove("hidden");
    $("pageTitle").textContent = "My projects";
});

$("navProjects").addEventListener("click", () => {
    $("backBtn").click();
});

$("refreshTasksBtn").addEventListener("click", loadTasks);

let tasks = [];

async function loadTasks() {
    if (!selectedProject) return;

    try {
        const projectId = idOf(selectedProject);
        const data = await api(`/projects/${projectId}/tasks`);

        tasks = normalizeArray(data);
        renderTasks();
    } catch (error) {
        showToast(error.message, true);
    }
}

function renderTasks() {
    $("taskCount").textContent = tasks.length;
    $("completedCount").textContent =
        tasks.filter((task) => task.status === "COMPLETED").length;

    const board = $("taskBoard");

    board.innerHTML = statuses.map((status) => {
        const columnTasks = tasks.filter(
            (task) => task.status === status.value
        );

        return `
            <section class="task-column">
                <div class="column-heading">
                    <span>${status.label}</span>
                    <span class="column-count">${columnTasks.length}</span>
                </div>
                <div class="column-tasks" data-column="${status.value}">
                    ${columnTasks.map((task) => {
                        const priority = task.priority || "MEDIUM";
                        return `
                            <article class="task-card"
                                     data-task-id="${escapeHTML(idOf(task))}">
                                <h3>${escapeHTML(task.title)}</h3>
                                <p>${escapeHTML(task.description || "No description")}</p>
                                <div class="task-meta">
                                    <span class="priority-${priority.toLowerCase()}">
                                        ${escapeHTML(priority)}
                                    </span>
                                    <span>${escapeHTML(formatDate(task.deadline))}</span>
                                </div>
                            </article>
                        `;
                    }).join("")}
                </div>
            </section>
        `;
    }).join("");

    board.querySelectorAll("[data-task-id]").forEach((card) => {
        card.addEventListener("click", () => {
            const task = tasks.find(
                (item) => String(idOf(item)) === card.dataset.taskId
            );

            if (task) openTaskDetails(task);
        });
    });
}

$("newTaskBtn").addEventListener("click", () => {
    if (!selectedProject) return;

    $("taskForm").reset();
    openDialog("taskDialog");
});

$("taskForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = event.submitter;
    if (button) button.disabled = true;

    const body = {
        title: $("taskTitle").value.trim(),
        description: $("taskDescription").value.trim(),
        priority: $("taskPriority").value
    };

    const deadline = $("taskDeadline").value;
    if (deadline) body.deadline = deadline;

    const assignee = $("taskAssignee").value.trim();
    if (assignee) body.assigneeId = Number(assignee);

    try {
        await api(`/projects/${idOf(selectedProject)}/tasks`, {
            method: "POST",
            body: JSON.stringify(body)
        });

        $("taskDialog").close();
        showToast("Task created successfully.");
        await loadTasks();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        if (button) button.disabled = false;
    }
});

function openTaskDetails(task) {
    selectedTask = task;

    $("detailTaskTitle").textContent = task.title || "Task";
    $("detailTaskDescription").textContent =
        task.description || "No description provided.";

    $("detailTaskStatus").value = task.status || "TODO";

    loadComments();
    loadActivities();
    loadAttachments();

    openDialog("taskDetailsDialog");
}

$("updateStatusBtn").addEventListener("click", async () => {
    if (!selectedProject || !selectedTask) return;

    const target = $("detailTaskStatus").value;

    try {
        await api(
            `/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/status`,
            {
                method: "PATCH",
                body: JSON.stringify({ status: target })
            }
        );

        showToast("Task status updated.");
        $("taskDetailsDialog").close();

        await loadTasks();
    } catch (error) {
        showToast(error.message, true);
    }
});

async function loadComments() {
    if (!selectedProject || !selectedTask) return;

    const container = $("commentsList");
    showEmpty(container, "Loading comments...");

    try {
        const data = await api(
            `/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/comments`
        );

        const comments = normalizeArray(data);

        if (!comments.length) {
            showEmpty(container, "No comments yet.");
            return;
        }

        container.innerHTML = comments.map((comment) => `
            <article class="comment-item">
                <strong>${escapeHTML(comment.authorName || "User")}</strong>
                <p>${escapeHTML(comment.content)}</p>
                <time>${escapeHTML(
                    comment.createdAt
                        ? new Date(comment.createdAt).toLocaleString()
                        : ""
                )}</time>
            </article>
        `).join("");
    } catch (error) {
        showEmpty(container, error.message);
    }
}

$("commentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    if (!selectedProject || !selectedTask) return;

    const content = $("commentContent").value.trim();
    if (!content) return;

    try {
        await api(
            `/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/comments`,
            {
                method: "POST",
                body: JSON.stringify({ content })
            }
        );

        $("commentContent").value = "";
        showToast("Comment posted.");
        await loadComments();
    } catch (error) {
        showToast(error.message, true);
    }
});

async function loadActivities() {
    if (!selectedProject || !selectedTask) return;

    const container = $("activityList");
    showEmpty(container, "Loading activity...");

    try {
        const data = await api(
            `/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/activities`
        );

        const activities = normalizeArray(data);

        if (!activities.length) {
            showEmpty(container, "No activity recorded yet.");
            return;
        }

        container.innerHTML = activities.map((activity) => `
            <article class="activity-item">
                <strong>${escapeHTML(activity.actorName || "User")}</strong>
                <p>
                    ${escapeHTML(activity.action || "ACTIVITY")}
                    ${activity.details
                        ? " — " + escapeHTML(activity.details)
                        : ""}
                </p>
                <time>${escapeHTML(
                    activity.createdAt
                        ? new Date(activity.createdAt).toLocaleString()
                        : ""
                )}</time>
            </article>
        `).join("");
    } catch (error) {
        showEmpty(container, error.message);
    }
}

async function initialize() {
    try {
        await loadCurrentUser();
    } catch {
        showAuth();
    }
}

setAuthMode("login");
initialize();



async function loadMembers() {
    if (!selectedProject) return;

    const container = $("membersList");
    showEmpty(container, "Loading project members...");

    try {
        const members = normalizeArray(
            await api(`/projects/${idOf(selectedProject)}/members`)
        );

        if (!members.length) {
            showEmpty(container, "No project members found.");
            return;
        }

        const role = getCurrentRole();
        const canManage = ["ADMIN", "PROJECT_MANAGER"].includes(role);

        container.innerHTML = members.map(member => {
            const name = member.name || member.email || "Member";
            const memberId = member.userId ?? member.id;
            const isCreator =
                String(member.email || "").toLowerCase() ===
                String(selectedProject.createdByEmail || "").toLowerCase();

            return `
                <article class="member-card">
                    <div class="avatar">
                        ${escapeHTML(name.charAt(0).toUpperCase())}
                    </div>
                    <div class="member-info">
                        <strong>${escapeHTML(name)}</strong>
                        <span>${escapeHTML(member.email || "")}</span>
                        <span>${escapeHTML(
                            (member.role || "USER").replaceAll("_", " ")
                        )}</span>
                    </div>
                    ${
                        canManage && !isCreator && memberId != null
                        ? `<button
                                class="btn secondary"
                                type="button"
                                data-remove-member="${escapeHTML(memberId)}"
                                data-member-name="${escapeHTML(name)}">
                                Remove
                           </button>`
                        : ""
                    }
                </article>
            `;
        }).join("");

        container.querySelectorAll("[data-remove-member]").forEach(button => {
            button.addEventListener("click", async () => {
                const userId = button.dataset.removeMember;
                const name = button.dataset.memberName;

                if (!confirm(`Remove ${name} from this project?`)) return;

                button.disabled = true;

                try {
                    await api(
                        `/projects/${idOf(selectedProject)}/members/${userId}`,
                        { method: "DELETE" }
                    );

                    showToast("Project member removed.");
                    await loadMembers();
                } catch (error) {
                    showToast(error.message || "Could not remove member.", true);
                    button.disabled = false;
                }
            });
        });
    } catch (error) {
        showEmpty(container, error.message);
    }
}



let selectedDeveloper = null;
let developerSearchTimer = null;

$("addMemberBtn").addEventListener("click", () => {
    if (!selectedProject) return;

    const role = getCurrentRole();

    if (!["ADMIN", "PROJECT_MANAGER"].includes(role)) {
        showToast("Only project managers or administrators can add members.", true);
        return;
    }

    $("memberForm").reset();
    $("developerSearchResults").innerHTML = "";
    $("selectedDeveloperEmail").value = "";
    $("developerSearchMessage").textContent =
        "Type at least 2 characters to search.";

    selectedDeveloper = null;
    openDialog("memberDialog");
});

$("developerSearch").addEventListener("input", () => {
    clearTimeout(developerSearchTimer);

    selectedDeveloper = null;
    $("selectedDeveloperEmail").value = "";

    const term = $("developerSearch").value.trim();
    const results = $("developerSearchResults");
    const message = $("developerSearchMessage");

    results.innerHTML = "";

    if (term.length < 2) {
        message.textContent = "Type at least 2 characters to search.";
        return;
    }

    message.textContent = "Searching...";

    developerSearchTimer = setTimeout(async () => {
        try {
            const users = normalizeArray(
                await api(
                    `/projects/${idOf(selectedProject)}/developer-options?q=${encodeURIComponent(term)}`
                )
            );

            results.innerHTML = "";

            if (!users.length) {
                message.textContent = "No available developers found.";
                return;
            }

            message.textContent = "Select a developer:";

            users.forEach(user => {
                const option = document.createElement("button");
                option.type = "button";
                option.className = "developer-search-option";
                option.textContent = `${user.name} (${user.email})`;

                option.addEventListener("click", () => {
                    selectedDeveloper = user;
                    $("selectedDeveloperEmail").value = user.email;
                    $("developerSearch").value =
                        `${user.name} (${user.email})`;

                    results.innerHTML = "";
                    message.textContent = "Selected: " + user.email;
                });

                results.appendChild(option);
            });
        } catch (error) {
            message.textContent = error.message || "Search failed.";
        }
    }, 250);
});

$("memberForm").addEventListener("submit", async event => {
    event.preventDefault();

    if (!selectedProject) return;

    if (!selectedDeveloper) {
        showToast("Search for and select a developer first.", true);
        return;
    }

    const button = event.submitter;
    if (button) button.disabled = true;

    try {
        await api(`/projects/${idOf(selectedProject)}/members`, {
            method: "POST",
            body: JSON.stringify({
                email: selectedDeveloper.email
            })
        });

        $("memberDialog").close();
        showToast("Member added successfully.");
        selectedDeveloper = null;

        await loadMembers();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        if (button) button.disabled = false;
    }
});


$("memberForm").addEventListener("submit", async event => {
    event.preventDefault();

    if (!selectedProject) return;

    const button = event.submitter;
    if (button) button.disabled = true;

    try {
        await api(`/projects/${idOf(selectedProject)}/members`, {
            method: "POST",
            body: JSON.stringify({
                email: $("memberEmail").value.trim()
            })
        });

        $("memberDialog").close();
        showToast("Member added successfully.");
        await loadMembers();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        if (button) button.disabled = false;
    }
});


async function loadAttachments() {
    if (!selectedProject || !selectedTask) return;

    const container = $("attachmentsList");
    showEmpty(container, "Loading attachments...");

    try {
        const attachments = normalizeArray(
            await api(
                `/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/attachments`
            )
        );

        if (!attachments.length) {
            showEmpty(container, "No attachments yet.");
            return;
        }

        container.innerHTML = attachments.map(item => `
            <article class="comment-item">
                <strong>${escapeHTML(item.originalFilename)}</strong>
                <p>${(Number(item.fileSize || 0) / 1024).toFixed(1)} KB</p>
                <button class="btn secondary"
                        type="button"
                        data-download-id="${escapeHTML(item.id)}">
                    Download
                </button>
            </article>
        `).join("");

        container.querySelectorAll("[data-download-id]").forEach(button => {
            button.addEventListener("click", async () => {
                const attachmentId = button.dataset.downloadId;

                try {
                    const response = await fetch(
                        `${API}/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/attachments/${attachmentId}`,
                        { credentials: "same-origin" }
                    );

                    if (!response.ok) {
                        throw new Error(`Download failed (${response.status})`);
                    }

                    const blob = await response.blob();
                    const objectUrl = URL.createObjectURL(blob);
                    const link = document.createElement("a");
                    link.href = objectUrl;
                    link.download = itemFilename(attachments, attachmentId);
                    document.body.appendChild(link);
                    link.click();
                    link.remove();
                    URL.revokeObjectURL(objectUrl);
                } catch (error) {
                    showToast(error.message, true);
                }
            });
        });
    } catch (error) {
        showEmpty(container, error.message);
    }
}

function itemFilename(attachments, attachmentId) {
    const item = attachments.find(
        attachment => String(attachment.id) === String(attachmentId)
    );
    return item?.originalFilename || "attachment";
}


$("attachmentForm").addEventListener("submit", async event => {
    event.preventDefault();

    if (!selectedProject || !selectedTask) return;

    const file = $("attachmentFile").files[0];

    if (!file) {
        showToast("Please choose a file.", true);
        return;
    }

    if (file.size > 5 * 1024 * 1024) {
        showToast("Maximum file size is 5 MB.", true);
        return;
    }

    const button = $("uploadAttachmentBtn");
    button.disabled = true;
    button.textContent = "Uploading...";

    const formData = new FormData();
    formData.append("file", file);

    try {
        const response = await fetch(
            `${API}/projects/${idOf(selectedProject)}/tasks/${idOf(selectedTask)}/attachments`,
            {
                method: "POST",
                credentials: "same-origin",
                body: formData
            }
        );

        if (!response.ok) {
            const body = await response.text();
            throw new Error(body || `Upload failed (${response.status})`);
        }

 $("attachmentForm").reset();

showToast("Attachment uploaded successfully.");

// Close the task details window after a successful upload.
$("taskDetailsDialog").close();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        button.disabled = false;
        button.textContent = "Upload attachment";
    }
});



$("cancelAttachmentBtn").addEventListener("click", () => {
    $("attachmentForm").reset();
});



