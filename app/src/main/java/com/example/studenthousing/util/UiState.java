package com.example.studenthousing.util;

public final class UiState<T> {
    public enum ScreenState { LOADING, CONTENT, EMPTY, ERROR }
    public enum SyncState { NOT_APPLICABLE, SYNCED, PENDING_SYNC, SYNC_ERROR }

    private final ScreenState screenState;
    private final T content;
    private final SyncState syncState;
    private final String errorMessage;

    public UiState(ScreenState screenState, T content, SyncState syncState, String errorMessage) {
        this.screenState = screenState;
        this.content = content;
        this.syncState = syncState;
        this.errorMessage = errorMessage;
    }

    public ScreenState getScreenState() { return screenState; }
    public T getContent() { return content; }
    public String getErrorMessage() { return errorMessage; }

    public static <T> UiState<T> loading() {
        return new UiState<>(ScreenState.LOADING, null, SyncState.NOT_APPLICABLE, null);
    }

    public static <T> UiState<T> content(T content, SyncState syncState) {
        ScreenState state = content == null ? ScreenState.EMPTY : ScreenState.CONTENT;
        return new UiState<>(state, content, syncState, null);
    }

    public static <T> UiState<T> error(String message) {
        return new UiState<>(ScreenState.ERROR, null, SyncState.NOT_APPLICABLE, message);
    }
}
