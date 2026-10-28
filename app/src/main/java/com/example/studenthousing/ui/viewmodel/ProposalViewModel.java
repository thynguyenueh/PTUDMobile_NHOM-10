package com.example.studenthousing.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class ProposalViewModel extends ViewModel {

    public static abstract class ProposalUiState {
        public static class Loading extends ProposalUiState {}
        public static class Success extends ProposalUiState {
            public final ExchangeRequest proposal;
            public Success(ExchangeRequest proposal) { this.proposal = proposal; }
        }
        public static class Signed extends ProposalUiState {
            public final String message;
            public Signed(String message) { this.message = message; }
        }
        public static class Error extends ProposalUiState {
            public final String error;
            public Error(String error) { this.error = error; }
        }
    }

    private final ExchangeRepository repository;
    private final MutableLiveData<ProposalUiState> uiState = new MutableLiveData<>(new ProposalUiState.Loading());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProposalViewModel(ExchangeRepository repository) {
        this.repository = repository;
    }

    public LiveData<ProposalUiState> getUiState() {
        return uiState;
    }

    public void loadProposalDetail(String proposalId) {
        uiState.setValue(new ProposalUiState.Loading());
        executor.execute(() -> {
            if (repository != null && proposalId != null) {
                repository.getById(proposalId, new AppCallback<ExchangeRequest>() {
                    @Override
                    public void onSuccess(ExchangeRequest data) {
                        uiState.postValue(new ProposalUiState.Success(data));
                    }

                    @Override
                    public void onError(Exception e) {
                        uiState.postValue(new ProposalUiState.Error(e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Lỗi tải đề xuất"));
                    }
                });
            } else {
                uiState.postValue(new ProposalUiState.Success(null));
            }
        });
    }

    public void signBilateralContract(String proposalId) {
        executor.execute(() -> {
            if (repository != null && proposalId != null) {
                repository.updateStatus(proposalId, Constants.REQ_EXCHANGING, new AppCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        uiState.postValue(new ProposalUiState.Signed("Biên bản đổi nhà đã được ký thành công!"));
                    }

                    @Override
                    public void onError(Exception e) {
                        uiState.postValue(new ProposalUiState.Error("Không thể ký hợp đồng: " + e.getMessage()));
                    }
                });
            } else {
                uiState.postValue(new ProposalUiState.Signed("Biên bản đổi nhà đã được ký thành công!"));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
