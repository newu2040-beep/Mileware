package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AnonymousPersona
import com.example.data.model.CommentModel
import com.example.data.model.ConfessionPost
import com.example.data.model.NotificationModel
import com.example.data.model.PastelTheme
import com.example.data.model.ReactionModel
import com.example.data.model.ReportModel
import com.example.data.model.UserModel
import com.example.data.repository.ConfessionRepository
import com.example.data.repository.UserRepository
import com.example.util.CardExportHelper
import com.example.util.HapticsHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class ConfessionViewModel(application: Application) : AndroidViewModel(application) {

    private val confessionRepo = ConfessionRepository(application)
    private val userRepo = UserRepository(application)
    private val context = application.applicationContext

    val currentUserId: String
        get() = Firebase.auth.currentUser?.uid ?: ""

    // Feed Filtering & Search
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedFeedTab = MutableStateFlow("latest") // latest, trending, popular, for_you
    val selectedFeedTab: StateFlow<String> = _selectedFeedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _appPastelTheme = MutableStateFlow(PastelTheme.LAVENDER_MIST)
    val appPastelTheme: StateFlow<PastelTheme> = _appPastelTheme.asStateFlow()

    // Transient UI status message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Export Dialog State
    private val _exportingPost = MutableStateFlow<ConfessionPost?>(null)
    val exportingPost: StateFlow<ConfessionPost?> = _exportingPost.asStateFlow()

    private val _exportTheme = MutableStateFlow(PastelTheme.LAVENDER_MIST)
    val exportTheme: StateFlow<PastelTheme> = _exportTheme.asStateFlow()

    private val _exportResolution = MutableStateFlow(CardExportHelper.ExportResolution.UHD_4K_PORTRAIT)
    val exportResolution: StateFlow<CardExportHelper.ExportResolution> = _exportResolution.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    // Real-time Reactions Map for Current User: (postId -> reactionType)
    val userReactions: StateFlow<Map<String, String>> = if (currentUserId.isNotBlank()) {
        confessionRepo.observeUserReactions(currentUserId)
            .catch { emit(emptyMap()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    } else {
        MutableStateFlow(emptyMap())
    }

    // Real-time Saved Post IDs Set
    val userSavedPostIds: StateFlow<Set<String>> = if (currentUserId.isNotBlank()) {
        confessionRepo.observeUserSavedPostIds(currentUserId)
            .catch { emit(emptySet()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
    } else {
        MutableStateFlow(emptySet())
    }

    // Real-time Feed Posts Stream
    @OptIn(ExperimentalCoroutinesApi::class)
    val feedPosts: StateFlow<UiState<List<ConfessionPost>>> = combine(
        _selectedCategory,
        _selectedFeedTab
    ) { category, tab -> category to tab }
        .flatMapLatest { (category, tab) ->
            confessionRepo.observeFeed(category, tab)
        }
        .combine(_searchQuery) { posts, query ->
            if (query.isBlank()) {
                posts
            } else {
                val q = query.trim().lowercase()
                posts.filter {
                    it.content.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.anonymousId.lowercase().contains(q) ||
                    it.postType.lowercase().contains(q)
                }
            }
        }
        .combine(userSavedPostIds) { posts, savedIds ->
            if (_selectedFeedTab.value == "for_you" && savedIds.isNotEmpty()) {
                // Boost content based on saved categories
                posts.sortedByDescending { if (savedIds.contains(it.postId)) 1 else 0 }
            } else {
                posts
            }
        }
        .combine(_toastMessage) { posts, _ -> posts }
        .combine(userReactions) { posts, _ -> posts }
        .mapPostsToUiState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    // Real-time User Confessions (My Posts)
    val myPosts: StateFlow<UiState<List<ConfessionPost>>> = if (currentUserId.isNotBlank()) {
        confessionRepo.observeMyPosts(currentUserId)
            .mapPostsToUiState()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    } else {
        MutableStateFlow(UiState.Success(emptyList()))
    }

    // Real-time Notifications
    val notifications: StateFlow<List<NotificationModel>> = if (currentUserId.isNotBlank()) {
        confessionRepo.observeNotifications(currentUserId)
            .catch { emit(emptyList()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    // User Profile
    val userProfile: StateFlow<UserModel?> = if (currentUserId.isNotBlank()) {
        userRepo.observeUser(currentUserId)
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        MutableStateFlow(null)
    }

    init {
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                val user = userRepo.getOrCreateUser(currentUserId)
                _appPastelTheme.value = PastelTheme.fromId(user.themePreference)
            }
        }
    }

    fun selectCategory(category: String) {
        HapticsHelper.playLightTick(context)
        _selectedCategory.value = category
    }

    fun selectFeedTab(tab: String) {
        HapticsHelper.playLightTick(context)
        _selectedFeedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDarkMode() {
        HapticsHelper.playClick(context)
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setAppPastelTheme(theme: PastelTheme) {
        HapticsHelper.playClick(context)
        _appPastelTheme.value = theme
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                userRepo.updateThemePreference(currentUserId, theme.id)
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun toggleReaction(post: ConfessionPost, reactionType: ReactionModel.Type) {
        HapticsHelper.playReactionPop(context)
        val currentReaction = userReactions.value[post.postId]
        val userAlias = userProfile.value?.anonymousHandle ?: "Anonymous Soul"

        viewModelScope.launch {
            confessionRepo.toggleReaction(
                postId = post.postId,
                postAuthorId = post.authorId,
                currentReactionType = currentReaction,
                newReactionType = reactionType.key,
                userAnonymousName = userAlias
            )
        }
    }

    fun toggleSavePost(postId: String) {
        val isCurrentlySaved = userSavedPostIds.value.contains(postId)
        HapticsHelper.playClick(context)
        viewModelScope.launch {
            val result = confessionRepo.toggleSavePost(postId, isCurrentlySaved)
            if (result.isSuccess) {
                val saved = result.getOrNull() == true
                _toastMessage.value = if (saved) "Added to private bookmarks ✨" else "Removed from bookmarks"
            }
        }
    }

    fun createConfession(
        content: String,
        category: String,
        postType: String,
        pastelTheme: PastelTheme,
        contentWarning: String,
        commentsEnabled: Boolean,
        reactionsEnabled: Boolean,
        mediaUrl: String,
        audioDurationSec: Int,
        customPersona: AnonymousPersona?,
        onSuccess: () -> Unit
    ) {
        HapticsHelper.playHeavyClick(context)
        val persona = customPersona ?: userProfile.value?.let {
            AnonymousPersona(it.anonymousHandle, it.anonymousAvatar, it.anonymousColor)
        } ?: AnonymousPersona.generateRandom()

        val newPost = ConfessionPost(
            anonymousId = persona.name,
            anonymousAvatar = persona.avatarEmoji,
            anonymousColor = persona.badgeColorHex,
            content = content.trim(),
            category = category,
            postType = postType,
            pastelTheme = pastelTheme.id,
            contentWarning = if (contentWarning == "None") "" else contentWarning,
            commentsEnabled = commentsEnabled,
            reactionsEnabled = reactionsEnabled,
            mediaUrl = mediaUrl,
            audioDurationSec = audioDurationSec
        )

        viewModelScope.launch {
            val result = confessionRepo.createPost(newPost)
            if (result.isSuccess) {
                HapticsHelper.playSuccess(context)
                _toastMessage.value = "Confession shared anonymously 🎭"
                onSuccess()
            } else {
                HapticsHelper.playError(context)
                _toastMessage.value = "Failed to post: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun addComment(
        postId: String,
        postAuthorId: String,
        content: String,
        parentId: String = "",
        onComplete: () -> Unit = {}
    ) {
        if (content.isBlank()) return
        HapticsHelper.playClick(context)

        val persona = userProfile.value?.let {
            AnonymousPersona(it.anonymousHandle, it.anonymousAvatar, it.anonymousColor)
        } ?: AnonymousPersona.generateRandom()

        viewModelScope.launch {
            val result = confessionRepo.addComment(
                postId = postId,
                postAuthorId = postAuthorId,
                commentText = content.trim(),
                anonymousId = persona.name,
                anonymousAvatar = persona.avatarEmoji,
                anonymousColor = persona.badgeColorHex,
                parentId = parentId
            )
            if (result.isSuccess) {
                HapticsHelper.playSuccess(context)
                onComplete()
            }
        }
    }

    fun deletePost(postId: String) {
        HapticsHelper.playHeavyClick(context)
        viewModelScope.launch {
            confessionRepo.deletePost(postId)
            _toastMessage.value = "Confession deleted"
        }
    }

    fun archivePost(postId: String, isArchived: Boolean) {
        HapticsHelper.playClick(context)
        viewModelScope.launch {
            confessionRepo.archivePost(postId, isArchived)
            _toastMessage.value = if (isArchived) "Confession archived" else "Confession unarchived"
        }
    }

    fun reportContent(targetType: String, targetId: String, reason: String, notes: String = "") {
        HapticsHelper.playClick(context)
        viewModelScope.launch {
            confessionRepo.reportContent(targetType, targetId, reason, notes)
            HapticsHelper.playSuccess(context)
            _toastMessage.value = "Thank you. Report submitted for review."
        }
    }

    fun blockUser(anonymousId: String) {
        HapticsHelper.playClick(context)
        viewModelScope.launch {
            confessionRepo.blockUser(anonymousId)
            _toastMessage.value = "Blocked $anonymousId"
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            confessionRepo.markNotificationRead(notificationId)
        }
    }

    fun updatePersona(persona: AnonymousPersona, bio: String) {
        HapticsHelper.playClick(context)
        if (currentUserId.isNotBlank()) {
            viewModelScope.launch {
                userRepo.updatePersona(
                    userId = currentUserId,
                    handle = persona.name,
                    avatar = persona.avatarEmoji,
                    color = persona.badgeColorHex,
                    bio = bio
                )
                _toastMessage.value = "Anonymous identity updated 🎭"
            }
        }
    }

    // Export High-Res Card
    fun openExportDialog(post: ConfessionPost) {
        HapticsHelper.playClick(context)
        _exportingPost.value = post
        _exportTheme.value = PastelTheme.fromId(post.pastelTheme)
    }

    fun closeExportDialog() {
        _exportingPost.value = null
    }

    fun setExportTheme(theme: PastelTheme) {
        HapticsHelper.playLightTick(context)
        _exportTheme.value = theme
    }

    fun setExportResolution(resolution: CardExportHelper.ExportResolution) {
        HapticsHelper.playLightTick(context)
        _exportResolution.value = resolution
    }

    fun saveExportCard(onSaved: (Uri) -> Unit) {
        val post = _exportingPost.value ?: return
        HapticsHelper.playHeavyClick(context)
        _isExporting.value = true

        viewModelScope.launch {
            try {
                val bitmap = CardExportHelper.renderConfessionCard(
                    post = post,
                    theme = _exportTheme.value,
                    resolution = _exportResolution.value
                )
                val saveResult = CardExportHelper.saveBitmapToGallery(
                    context = context,
                    bitmap = bitmap,
                    resolutionLabel = _exportResolution.value.badge
                )
                _isExporting.value = false
                if (saveResult.isSuccess) {
                    HapticsHelper.playSuccess(context)
                    _toastMessage.value = "Saved ${_exportResolution.value.badge} card to Gallery! 🖼️"
                    saveResult.getOrNull()?.let(onSaved)
                } else {
                    HapticsHelper.playError(context)
                    _toastMessage.value = "Failed to save: ${saveResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _isExporting.value = false
                HapticsHelper.playError(context)
                _toastMessage.value = "Export error: ${e.message}"
            }
        }
    }

    fun shareExportCard(onIntentReady: (android.content.Intent) -> Unit) {
        val post = _exportingPost.value ?: return
        HapticsHelper.playClick(context)
        _isExporting.value = true

        viewModelScope.launch {
            try {
                val bitmap = CardExportHelper.renderConfessionCard(
                    post = post,
                    theme = _exportTheme.value,
                    resolution = _exportResolution.value
                )
                val shareResult = CardExportHelper.shareBitmap(
                    context = context,
                    bitmap = bitmap,
                    caption = "\"${post.content.take(120)}...\""
                )
                _isExporting.value = false
                if (shareResult.isSuccess) {
                    shareResult.getOrNull()?.let(onIntentReady)
                } else {
                    _toastMessage.value = "Share failed: ${shareResult.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _isExporting.value = false
                _toastMessage.value = "Share error: ${e.message}"
            }
        }
    }
}

private fun Flow<List<ConfessionPost>>.mapPostsToUiState(): Flow<UiState<List<ConfessionPost>>> {
    return this.map<List<ConfessionPost>, UiState<List<ConfessionPost>>> { posts ->
        UiState.Success(posts)
    }.catch { error ->
        emit(UiState.Error(error.localizedMessage ?: "Failed to load confessions"))
    }
}
