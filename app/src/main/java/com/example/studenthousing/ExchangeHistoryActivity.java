package com.example.studenthousing;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studenthousing.data.model.ExchangeRequest;
import com.example.studenthousing.data.repository.ExchangeRepository;
import com.example.studenthousing.util.AppCallback;
import com.example.studenthousing.util.SessionManager;

import java.util.List;

public class ExchangeHistoryActivity extends AppCompatActivity {

    private SessionManager session;
    private ExchangeRepository exchangeRepository;
    private LinearLayout layoutExchangesContainer;
    private TextView tvTotalExchanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange_history);

        session = new SessionManager(this);
        exchangeRepository = new ExchangeRepository(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        layoutExchangesContainer = findViewById(R.id.layoutExchangesContainer);
        tvTotalExchanges = findViewById(R.id.tvTotalExchanges);

        View btnCreateProposal = findViewById(R.id.btnCreateProposal);
        if (btnCreateProposal != null) {
            btnCreateProposal.setOnClickListener(v ->
                    startActivity(new Intent(ExchangeHistoryActivity.this, ProposalActivity.class)));
        }

        loadExchangeHistory();
    }

    private void loadExchangeHistory() {
        String userId = session.getUserId() != null ? session.getUserId() : "current_user";

        exchangeRepository.getBySender(userId, new AppCallback<List<ExchangeRequest>>() {
            @Override
            public void onSuccess(List<ExchangeRequest> list) {
                runOnUiThread(() -> displayExchanges(list));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> displayExchanges(null));
            }
        });
    }

    private void displayExchanges(List<ExchangeRequest> list) {
        if (layoutExchangesContainer == null) return;
        layoutExchangesContainer.removeAllViews();

        if (list != null && !list.isEmpty()) {
            tvTotalExchanges.setText(list.size() + " lượt đổi nhà đã ghi nhận");
            for (ExchangeRequest req : list) {
                addExchangeItemView(
                        "Đổi nhà #" + req.getRequestId().substring(0, Math.min(6, req.getRequestId().length())),
                        "Đối tác ID: " + req.getReceiverId(),
                        req.getStatus() != null ? req.getStatus() : "Hoàn tất",
                        "Thời gian: " + (req.getExchangePeriod() != null ? req.getExchangePeriod() : "Học kỳ 1"),
                        req.getMessage() != null ? req.getMessage() : "Thỏa thuận đổi nhà đã ký kết.",
                        req.getReceiverId(),
                        req.getRequestId()
                );
            }
        } else {
            // Mẫu nếu chưa có dữ liệu DB/Firestore
            tvTotalExchanges.setText("3 lượt đổi nhà đã hoàn tất");

            addExchangeItemView("Căn hộ Thảo Điền, Q.2",
                    "Gia đình Cô Mai",
                    "✓ Hoàn tất",
                    "Thời gian: Học kỳ 1 (09/2026 - 01/2027)",
                    "Thỏa thuận đổi nhà 2 chiều đã hoàn tất thành công.",
                    "user_co_mai", "req_001");

            addExchangeItemView("Nhà trọ Linh Trung, Thủ Đức",
                    "Gia đình Chú Nam",
                    "✓ Hoàn tất",
                    "Thời gian: Học kỳ 2 (02/2026 - 06/2026)",
                    "Thỏa thuận đổi nhà cho sinh viên đã hoàn tất.",
                    "user_chu_nam", "req_002");

            addExchangeItemView("Phòng trọ Linh Xuân, Thủ Đức",
                    "Bạn Tuấn (ĐH Bách Khoa)",
                    "✓ Hoàn tất",
                    "Thời gian: Học kỳ Hè (07/2026 - 08/2026)",
                    "Đổi phòng thời hạn ngắn hoàn tất.",
                    "user_tuan", "req_003");
        }
    }

    private void addExchangeItemView(String title, String partner, String status, String period, String message, String targetUserId, String requestId) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View itemView = inflater.inflate(R.layout.item_exchange_history, layoutExchangesContainer, false);

        TextView tvTitle = itemView.findViewById(R.id.tvExchangeTitle);
        TextView tvPartner = itemView.findViewById(R.id.tvExchangePartner);
        TextView tvStatus = itemView.findViewById(R.id.tvExchangeStatus);
        TextView tvPeriod = itemView.findViewById(R.id.tvExchangePeriod);
        TextView tvMessage = itemView.findViewById(R.id.tvExchangeMessage);
        View btnReviewPartner = itemView.findViewById(R.id.btnReviewPartner);

        tvTitle.setText(title);
        tvPartner.setText(partner.startsWith("Đối tác") ? partner : "Đối tác: " + partner);
        tvStatus.setText(status);
        tvPeriod.setText(period);
        tvMessage.setText(message);

        if (btnReviewPartner != null) {
            btnReviewPartner.setOnClickListener(v -> {
                Intent intent = new Intent(ExchangeHistoryActivity.this, ReviewActivity.class);
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_ID, requestId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ID, targetUserId);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_NAME, partner);
                intent.putExtra(ReviewActivity.EXTRA_TARGET_USER_ROLE, "ĐỐI TÁC ĐỔI NHÀ");
                intent.putExtra(ReviewActivity.EXTRA_REQUEST_INFO, period + " · " + status);
                startActivity(intent);
            });
        }

        layoutExchangesContainer.addView(itemView);
    }
}
