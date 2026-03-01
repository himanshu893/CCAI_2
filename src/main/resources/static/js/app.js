/* =====================================================
   SkyLock CloudCmd — App Logic
   ===================================================== */

(function () {
    'use strict';

    // ── State ────────────────────────────────────────
    let selectedFileId = null;
    let currentFolderId = null;
    let dashboardData = null;
    let folderStack = []; // [{id, name}, ...] for breadcrumb navigation

    // ── DOM Refs ─────────────────────────────────────
    const assetGrid = document.getElementById('asset-grid');
    const inspectorPanel = document.getElementById('inspector-panel');
    const uploadModal = document.getElementById('upload-modal');
    const folderModal = document.getElementById('folder-modal');
    const toastEl = document.getElementById('toast');

    // ── Init ─────────────────────────────────────────
    document.addEventListener('DOMContentLoaded', () => {
        loadDashboard();
        bindSidebarNav();
        bindUploadModal();
        bindFolderModal();
        bindUserMenu();
        bindStorageRing();
    });

    // ── API Helpers ──────────────────────────────────
    async function apiFetch(url, options = {}) {
        try {
            const res = await fetch(url, options);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return await res.json();
        } catch (e) {
            showToast(e.message || 'Network error', 'error');
            throw e;
        }
    }

    // ── Dashboard Load ───────────────────────────────
    async function loadDashboard() {
        let url = '/api/dashboard';
        if (currentFolderId) url += '?folderId=' + encodeURIComponent(currentFolderId);

        try {
            dashboardData = await apiFetch(url);
            renderGrid(dashboardData.files, dashboardData.folders);
            updateStorage(dashboardData.storage);
            updateBreadcrumbs();
            updateContentHeader();
        } catch (e) {
            renderEmptyState('Failed to load files.');
        }
    }

    // ── Render Grid ──────────────────────────────────
    function renderGrid(files, folders) {
        assetGrid.innerHTML = '';

        if ((!files || files.length === 0) && (!folders || folders.length === 0)) {
            renderEmptyState('No files yet. Click "New Upload" or "New Folder" to get started.');
            return;
        }

        let index = 0;

        // Render folders first
        if (folders) {
            folders.forEach(folder => {
                const card = createFolderCard(folder, index);
                assetGrid.appendChild(card);
                index++;
            });
        }

        // Then files
        if (files) {
            files.forEach(file => {
                const card = createFileCard(file, index);
                assetGrid.appendChild(card);
                index++;
            });
        }
    }

    function createFolderCard(folder, index) {
        const card = document.createElement('div');
        card.className = 'asset-card';
        card.style.animationDelay = `${index * 0.06}s`;
        card.dataset.folderId = folder.id;

        card.innerHTML = `
            <div class="card-thumb">
                <span class="folder-icon">📁</span>
            </div>
            <div class="card-info">
                <h3>${escapeHtml(folder.name)}</h3>
                <span class="card-meta">${folder.itemCount || 0} items · Folder</span>
            </div>
        `;

        card.addEventListener('click', () => {
            folderStack.push({ id: folder.id, name: folder.name });
            currentFolderId = folder.id;
            selectedFileId = null;
            closeInspector();
            loadDashboard();
        });

        return card;
    }

    function createFileCard(file, index) {
        const card = document.createElement('div');
        card.className = 'asset-card';
        card.style.animationDelay = `${index * 0.06}s`;
        card.dataset.fileId = file.id;

        const thumbContent = getThumbContent(file);

        card.innerHTML = `
            <div class="card-thumb">
                ${thumbContent}
                <div class="card-actions">
                    <button class="card-action-btn edit" title="Edit" onclick="event.stopPropagation()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                    </button>
                    <button class="card-action-btn share" title="Share" onclick="event.stopPropagation()">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                            <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                            <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                        </svg>
                    </button>
                    <button class="card-action-btn delete" title="Delete" data-file-id="${file.id}" onclick="event.stopPropagation(); window.skylock.deleteFile('${file.id}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                        </svg>
                    </button>
                </div>
            </div>
            <div class="card-info">
                <h3>${escapeHtml(file.filename)}</h3>
                <span class="card-meta">${formatSize(file.size)} · Updated ${formatDate(file.uploadedAt)}</span>
            </div>
        `;

        card.addEventListener('click', () => selectFile(file, card));

        return card;
    }

    function getThumbContent(file) {
        const icons = {
            image: '🖼️',
            pdf: '📄',
            document: '📝',
            spreadsheet: '📊',
            video: '🎬',
            audio: '🎵',
            archive: '📦',
            file: '📎'
        };
        const icon = icons[file.type] || icons.file;
        return `<span class="file-icon">${icon}</span>`;
    }

    // ── File Selection ───────────────────────────────
    function selectFile(file, card) {
        // Deselect previous
        document.querySelectorAll('.asset-card.selected').forEach(c => c.classList.remove('selected'));
        card.classList.add('selected');

        selectedFileId = file.id;
        populateInspector(file);
        openInspector();
    }

    function populateInspector(file) {
        const previewIcon = getPreviewIcon(file);
        document.getElementById('inspector-preview').innerHTML = previewIcon;
        document.getElementById('inspector-filename').textContent = file.filename;
        document.getElementById('inspector-type').textContent = (file.type || 'File').toUpperCase() + ' File';
        document.getElementById('inspector-size').textContent = formatSize(file.size);
        document.getElementById('inspector-created').textContent = formatDate(file.uploadedAt);

        // Download link
        const dlBtn = document.getElementById('btn-download');
        dlBtn.onclick = () => {
            window.location.href = '/api/files/download/' + file.id;
        };

        // Tags
        const tagRow = document.getElementById('inspector-tags');
        tagRow.innerHTML = '';
        const type = file.type || 'file';
        const typeTag = document.createElement('span');
        typeTag.className = 'tag ' + type;
        typeTag.textContent = type.toUpperCase();
        tagRow.appendChild(typeTag);
    }

    function getPreviewIcon(file) {
        if (file.type === 'image') {
            return `<span class="neon-icon">🖼️</span>`;
        }
        const icons = {
            pdf: '📄', document: '📝', spreadsheet: '📊',
            video: '🎬', audio: '🎵', archive: '📦', file: '📎'
        };
        return `<span class="neon-icon">${icons[file.type] || '📎'}</span>`;
    }

    // ── Inspector Open / Close ───────────────────────
    function openInspector() {
        inspectorPanel.classList.add('open');
    }

    function closeInspector() {
        inspectorPanel.classList.remove('open');
        selectedFileId = null;
        document.querySelectorAll('.asset-card.selected').forEach(c => c.classList.remove('selected'));
    }

    // Expose to HTML onclick
    window.skylock = window.skylock || {};
    window.skylock.closeInspector = closeInspector;

    // ── Delete File ──────────────────────────────────
    window.skylock.deleteFile = async function (fileId) {
        if (!confirm('Are you sure you want to delete this file?')) return;

        try {
            await apiFetch('/api/files/' + fileId, { method: 'DELETE' });
            showToast('File deleted successfully', 'success');
            if (selectedFileId === fileId) closeInspector();
            loadDashboard();
        } catch (e) {
            // Error already shown by apiFetch
        }
    };

    // ── Breadcrumb Navigation ────────────────────────
    function updateBreadcrumbs() {
        const breadcrumbs = document.getElementById('breadcrumbs');
        if (!breadcrumbs) return;

        let html = `<a href="javascript:void(0)" onclick="window.skylock.goToRoot()">Home</a>`;

        if (folderStack.length > 0) {
            html += `<span class="sep">/</span>`;
            html += `<a href="javascript:void(0)" onclick="window.skylock.goToRoot()">My Files</a>`;

            folderStack.forEach((folder, index) => {
                html += `<span class="sep">/</span>`;
                if (index === folderStack.length - 1) {
                    // Current folder (not clickable)
                    html += `<span class="current">${escapeHtml(folder.name)}</span>`;
                } else {
                    html += `<a href="javascript:void(0)" onclick="window.skylock.goToFolder(${index})">${escapeHtml(folder.name)}</a>`;
                }
            });
        } else {
            html += `<span class="sep">/</span>`;
            html += `<span class="current">My Files</span>`;
        }

        breadcrumbs.innerHTML = html;
    }

    function updateContentHeader() {
        const header = document.querySelector('.content-header h1');
        if (!header) return;

        if (folderStack.length > 0) {
            header.textContent = folderStack[folderStack.length - 1].name;
        } else {
            header.textContent = 'My Files';
        }
    }

    window.skylock.goToRoot = function () {
        folderStack = [];
        currentFolderId = null;
        closeInspector();
        loadDashboard();
    };

    window.skylock.goToFolder = function (index) {
        // Navigate to a specific folder in the stack
        const target = folderStack[index];
        folderStack = folderStack.slice(0, index + 1);
        currentFolderId = target.id;
        closeInspector();
        loadDashboard();
    };

    window.skylock.goBack = function () {
        if (folderStack.length > 0) {
            folderStack.pop();
            if (folderStack.length > 0) {
                currentFolderId = folderStack[folderStack.length - 1].id;
            } else {
                currentFolderId = null;
            }
            closeInspector();
            loadDashboard();
        }
    };

    // ── Sidebar Nav ──────────────────────────────────
    function bindSidebarNav() {
        const navItems = document.querySelectorAll('.nav-item');
        navItems.forEach(item => {
            item.addEventListener('click', function () {
                navItems.forEach(n => n.classList.remove('active'));
                this.classList.add('active');

                // Handle "Home" click — go to root
                if (this.dataset.nav === 'home' || this.dataset.nav === 'projects') {
                    folderStack = [];
                    currentFolderId = null;
                    closeInspector();
                    loadDashboard();
                }
            });
        });
    }

    // ── Upload Modal ─────────────────────────────────
    function bindUploadModal() {
        const uploadBtn = document.getElementById('upload-btn');
        const closeBtn = document.getElementById('close-upload');
        const cancelBtn = document.getElementById('cancel-upload');
        const confirmBtn = document.getElementById('confirm-upload');
        const dropZone = document.getElementById('drop-zone');
        const fileInput = document.getElementById('file-input');
        const selectedFileName = document.getElementById('selected-file-name');

        if (uploadBtn) {
            uploadBtn.addEventListener('click', () => {
                uploadModal.classList.add('visible');
            });
        }

        if (closeBtn) closeBtn.addEventListener('click', closeUploadModal);
        if (cancelBtn) cancelBtn.addEventListener('click', closeUploadModal);

        // Click on overlay to close
        if (uploadModal) {
            uploadModal.addEventListener('click', (e) => {
                if (e.target === uploadModal) closeUploadModal();
            });
        }

        // Drop zone
        if (dropZone) {
            dropZone.addEventListener('click', () => fileInput.click());

            dropZone.addEventListener('dragover', (e) => {
                e.preventDefault();
                dropZone.classList.add('dragover');
            });
            dropZone.addEventListener('dragleave', () => dropZone.classList.remove('dragover'));
            dropZone.addEventListener('drop', (e) => {
                e.preventDefault();
                dropZone.classList.remove('dragover');
                if (e.dataTransfer.files.length > 0) {
                    fileInput.files = e.dataTransfer.files;
                    showSelectedFile(e.dataTransfer.files[0].name);
                }
            });
        }

        if (fileInput) {
            fileInput.addEventListener('change', () => {
                if (fileInput.files.length > 0) {
                    showSelectedFile(fileInput.files[0].name);
                }
            });
        }

        if (confirmBtn) {
            confirmBtn.addEventListener('click', async () => {
                if (!fileInput.files.length) {
                    showToast('Please select a file first', 'error');
                    return;
                }
                await uploadFile(fileInput.files[0]);
            });
        }

        function showSelectedFile(name) {
            if (selectedFileName) {
                selectedFileName.textContent = '📎 ' + name;
                selectedFileName.style.display = 'block';
            }
        }
    }

    async function uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);
        if (currentFolderId) formData.append('folderId', currentFolderId);

        const progressBar = document.getElementById('upload-progress');
        const progressFill = document.getElementById('progress-fill');

        if (progressBar) {
            progressBar.classList.add('active');
            progressFill.style.width = '30%';
        }

        try {
            const res = await fetch('/upload', {
                method: 'POST',
                body: formData
            });

            if (progressFill) progressFill.style.width = '100%';

            if (res.ok) {
                showToast('File uploaded successfully!', 'success');
                closeUploadModal();
                loadDashboard();
            } else {
                const text = await res.text();
                showToast(text || 'Upload failed', 'error');
            }
        } catch (e) {
            showToast('Upload failed: ' + e.message, 'error');
        } finally {
            if (progressBar) {
                setTimeout(() => {
                    progressBar.classList.remove('active');
                    if (progressFill) progressFill.style.width = '0';
                }, 500);
            }
        }
    }

    function closeUploadModal() {
        uploadModal.classList.remove('visible');
        const fileInput = document.getElementById('file-input');
        const selectedFileName = document.getElementById('selected-file-name');
        if (fileInput) fileInput.value = '';
        if (selectedFileName) selectedFileName.style.display = 'none';
    }

    // ── Folder Modal ─────────────────────────────────
    function bindFolderModal() {
        const folderBtn = document.getElementById('new-folder-btn');
        const cancelBtn = document.getElementById('cancel-folder');
        const confirmBtn = document.getElementById('confirm-folder');
        const nameInput = document.getElementById('folder-name-input');

        if (folderBtn) {
            folderBtn.addEventListener('click', () => {
                if (folderModal) {
                    folderModal.classList.add('visible');
                    if (nameInput) {
                        nameInput.value = '';
                        nameInput.focus();
                    }
                }
            });
        }

        if (cancelBtn) cancelBtn.addEventListener('click', closeFolderModal);

        // Click on overlay to close
        if (folderModal) {
            folderModal.addEventListener('click', (e) => {
                if (e.target === folderModal) closeFolderModal();
            });
        }

        // Enter key to submit
        if (nameInput) {
            nameInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') createNewFolder();
            });
        }

        if (confirmBtn) {
            confirmBtn.addEventListener('click', createNewFolder);
        }
    }

    async function createNewFolder() {
        const nameInput = document.getElementById('folder-name-input');
        const name = nameInput ? nameInput.value.trim() : '';

        if (!name) {
            showToast('Please enter a folder name', 'error');
            return;
        }

        try {
            const body = { name: name };
            if (currentFolderId) body.parentId = currentFolderId;

            await apiFetch('/api/folders', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });

            showToast('Folder created successfully!', 'success');
            closeFolderModal();
            loadDashboard();
        } catch (e) {
            // Error already shown by apiFetch
        }
    }

    function closeFolderModal() {
        if (folderModal) folderModal.classList.remove('visible');
        const nameInput = document.getElementById('folder-name-input');
        if (nameInput) nameInput.value = '';
    }

    // ── User Menu ────────────────────────────────────
    function bindUserMenu() {
        const avatarBtn = document.getElementById('avatar-btn');
        const dropdown = document.getElementById('user-dropdown');

        if (avatarBtn && dropdown) {
            avatarBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                dropdown.classList.toggle('open');
            });

            document.addEventListener('click', () => {
                dropdown.classList.remove('open');
            });
        }
    }

    // ── Storage Ring ─────────────────────────────────
    function bindStorageRing() {
        // Will be called with actual data after dashboard load
    }

    function updateStorage(storage) {
        const ringFill = document.getElementById('ring-fill');
        const ringText = document.getElementById('ring-text');
        const storageUsed = document.getElementById('storage-used');

        if (!storage) return;

        const pct = storage.percentage || 0;
        const circumference = 2 * Math.PI * 17; // r=17

        if (ringFill) {
            ringFill.style.strokeDasharray = circumference;
            ringFill.style.strokeDashoffset = circumference;
            // Animate after a short delay
            setTimeout(() => {
                ringFill.style.strokeDashoffset = circumference - (circumference * pct / 100);
            }, 300);
        }

        if (ringText) ringText.textContent = pct + '%';
        if (storageUsed) storageUsed.textContent = formatSize(storage.used) + ' of ' + formatSize(storage.max);
    }

    // ── Empty State ──────────────────────────────────
    function renderEmptyState(msg) {
        assetGrid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/>
                </svg>
                <h3>No files here</h3>
                <p>${msg}</p>
            </div>
        `;
    }

    // ── Toast ────────────────────────────────────────
    function showToast(msg, type) {
        if (!toastEl) return;
        toastEl.textContent = msg;
        toastEl.className = 'toast ' + type + ' visible';

        setTimeout(() => {
            toastEl.classList.remove('visible');
        }, 3000);
    }

    // ── Utilities ────────────────────────────────────
    function formatSize(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function formatDate(dateStr) {
        if (!dateStr) return 'Unknown';
        try {
            const d = new Date(dateStr);
            const now = new Date();
            const diff = now - d;
            const mins = Math.floor(diff / 60000);
            if (mins < 1) return 'Just now';
            if (mins < 60) return mins + 'm ago';
            const hrs = Math.floor(mins / 60);
            if (hrs < 24) return hrs + 'h ago';
            const days = Math.floor(hrs / 24);
            if (days < 7) return days + 'd ago';
            return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        } catch {
            return dateStr;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

})();
