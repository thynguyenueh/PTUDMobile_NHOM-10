package com.example.studenthousing.ui.exchange;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studenthousing.R;
import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.model.User;
import com.example.studenthousing.data.repository.UserRepository;
import com.example.studenthousing.databinding.ActivityExchangeHistoryBinding;
import com.example.studenthousing.ui.adapter.ExchangeHistoryAdapter;
import com.example.studenthousing.ui.review.ReviewActivity;
import com.example.studenthousing.ui.viewmodel.ExchangeHistoryViewModel;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.Constants;
import com.example.studenthousing.util.SessionManager;
import com.example.studenthousing.util.UiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExchangeHistoryActivity extends AppCompatActivity implements ExchangeHistoryAdapter.OnExchangeActionListener {

    private ActivityExchangeHistoryBinding binding;
    private SessionManager session;
    private ExchangeHistoryViewModel viewModel;
    private ExchangeHistoryAdapter exchangeAdapter;
    private List<ExchangeRequest> allRequests = new ArrayList<>();
    private int currentCheckedChipId = R.id.chipExchangeAll;
    private final Map<String, String> cachedUserNames = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityExchangeHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        session = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ExchangeHistoryViewModel.class);
        viewModel.getState().observe(this, this::renderState);

        binding.btnBack.setOnClickListener(v -> finish());

        String currentUserId = session.getUserId() != null ? session.getUserId() : "";
        exchangeAdapter = new ExchangeHistoryAdapter(this, currentUserId);

        if (binding.chipGroupExchangeStatus != null) {
            binding.chipGroupExchangeStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    currentCheckedChipId = checkedIds.get(0);
                    applyFilter(currentCheckedChipId);
                }
            });
        }

        if (binding.btnCreateProposal != null) {
            binding.btnCreateProposal.setOnClickListener(v ->
                    startActivity(new Intent(ExchangeHistoryActivity.this, ProposalActivity.class)));
        }

        loadExchangeHistory();
    }

    private void loadExchangeHistory() {
        String userId = session.getUserId();
        viewModel.observeExchangeHistory(userId);
    }

    private void renderState(UiState<ExchangeHistoryViewModel.ExchangeHistoryData> state) {
        if (state == null || state.getContent() == null) {
            binding.tvTotalExchanges.setText("0 lượt");
            if (exchangeAdapter != null) exchangeAdapter.submitList(new ArrayList<>());
            return;
        }

        ExchangeHistoryViewModel.ExchangeHistoryData data = state.getContent();
        allRequests = data.allRequests;
        cachedUserNames.clear();
        cachedUserNames.putAll(data.userNames);
        if (exchangeAdapter != null) {
            exchangeAdapter.setCache(data.userNames, data.postTitles);
        }
        applyFilter(currentCheckedChipId);
    }

    private void displayExchanges(List<ExchangeRequest> list) {
        binding.tvTotalExchanges.setText(allRequests.size() + " lượt đổi");
        if (exchangeAdapter != null) exchangeAdapter.submitList(list != null ? list : new ArrayList<>());
    }

    private void applyFilter(int checkedChipId) {
        if (checkedChipId == R.id.chipExchangeAll) {
            displayExchanges(allRequests);
            return;
        }
        List<ExchangeRequest> filtered = new ArrayList<>();
        for (ExchangeRequest req : allRequests) {
            String status = req.getStatus();
            if (checkedChipId == R.id.chipExchangePending && Constants.REQ_PENDING.equalsIgnoreCase(status)) {
                filtered.add(req);
            } else if (checkedChipId == R.id.chipExchangeApproved
                    && Constants.REQ_EXCHANGING.equalsIgnoreCase(status)) {
                filtered.add(req);
            } else if (checkedChipId == R.id.chipExchangeCompleted
                    && Constants.REQ_COMPLETED.equalsIgnoreCase(status)) {
                filtered.add(req);
            } else if (checkedChipId == R.id.chipExchangeClosed
                    && (Constants.REQ_REJECTED.equalsIgnoreCase(status)
                    || Constants.REQ_CANCELLED.equalsIgnoreCase(status)
                    || Constants.REQ_WITHDRAWN.equalsIgnoreCase(status)
                    || Constants.REQ_EXPIRED.equalsIgnoreCase(status))) {
                filtered.add(req);
            }
        }
        displayExchanges(filtered);
    }

    @Override
    public void onAcceptExchange(@NonNull ExchangeRequest request) {
        String currentUserId = session.getUserId() != null ? session.getUserId() : "current_user";
        viewModel.acceptExchange(request, currentUserId, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(ExchangeHistoryActivity.this, "Đã đồng ý đề xuất đổi nhà!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ExchangeHistoryActivity.this, "Không thể đồng ý đề xuất: " + (e == null ? "Lỗi" : e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRejectExchange(@NonNull ExchangeRequest request) {
        viewModel.rejectExchange(request, new AppCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Toast.makeText(ExchangeHistoryActivity.this, "Đã từ chối đề xuất đổi nhà.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ExchangeHistoryActivity.this, "Không thể từ chối đề xuất: " + (e == null ? "Lỗi" : e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getPartnerName(String partnerId) {
        String cached = cachedUserNames.get(partnerId);
        if (cached != null && !cached.trim().isEmpty() && !cached.equals(partnerId)) return cached;
        User user = new com.example.studenthousing.data.local.UserDao(this).getById(partnerId);
        if (user != null && user.getFamilyName() != null && !user.getFamilyName().trim().isEmpty()) return user.getFamilyName();
        return getString(R.string.chat_default_partner_name);
    }

    @Override
    public void onReviewPartner(@NonNull ExchangeRequest request) {
        String currentUserId = session.getUserId() != null ? session.getUserId() : "current_user";
        boolean isInbound = currentUserId.equals(request.getReceiverId());
        String partnerId = isInbound ? request.getSenderId() : request.getReceiverId();
        UserRepository users = new UserRepository(this);
        users.getUserProfile(partnerId, new AppCallback<User>() {
            @Override public void onSuccess(User user) {
                String name = user != null && user.getFamilyName() != null ? user.getFamilyName() : getPartnerName(partnerId);
                Intent intent = new Intent(ExchangeHistoryActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_ID, request.getRequestId());
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, partnerId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, name);
                startActivity(intent);
            }
            @Override public void onError(Exception e) {
                Intent intent = new Intent(ExchangeHistoryActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_ID, request.getRequestId());
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, partnerId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, getPartnerName(partnerId));
                startActivity(intent);
            }
        });
    }

    @Override
    public void onViewAgreement(@NonNull ExchangeRequest request) {
        ProposalDetailActivity.start(this, request.getRequestId(), request.getAgreementText());
    }
}
