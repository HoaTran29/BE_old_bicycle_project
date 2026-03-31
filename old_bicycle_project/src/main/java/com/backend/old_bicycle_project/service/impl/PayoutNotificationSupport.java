package com.backend.old_bicycle_project.service.impl;

import com.backend.old_bicycle_project.config.NotificationEvent;
import com.backend.old_bicycle_project.entity.Payout;
import com.backend.old_bicycle_project.entity.User;
import com.backend.old_bicycle_project.entity.enums.AppRole;
import com.backend.old_bicycle_project.entity.enums.NotificationType;
import com.backend.old_bicycle_project.entity.enums.PayoutStatus;
import com.backend.old_bicycle_project.entity.enums.PayoutType;
import com.backend.old_bicycle_project.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

final class PayoutNotificationSupport {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    PayoutNotificationSupport(ApplicationEventPublisher eventPublisher, UserRepository userRepository) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
    }

    void publishPayoutAwaitingNotification(Payout payout) {
        if (payout.getType() == PayoutType.refund) {
            if (payout.getStatus() == PayoutStatus.profile_required) {
                publishProfileRequiredNotification(payout, false);
                return;
            }

            publishOrderNotification(
                    payout.getRecipient().getId(),
                    "Yêu cầu hoàn tiền đã được duyệt",
                    "Admin đã duyệt hoàn tiền. Hệ thống đang chờ chuyển khoản thủ công cho bạn.",
                    "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
            );
            publishAdminPayoutPendingNotification(
                    payout,
                    "Có payout hoàn tiền cần chuyển khoản",
                    "Một yêu cầu hoàn tiền đã được duyệt và đang chờ admin chuyển khoản thủ công."
            );
            return;
        }

        if (payout.getStatus() == PayoutStatus.profile_required) {
            publishProfileRequiredNotification(payout, false);
            return;
        }

        publishOrderNotification(
                payout.getRecipient().getId(),
                "Khoản cọc đang chờ giải ngân",
                "Đơn hàng đã hoàn tất và admin sẽ chuyển khoản thủ công khoản cọc cho bạn.",
                "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        publishAdminPayoutPendingNotification(
                payout,
                "Có payout cho người bán cần giải ngân",
                "Một đơn hàng đã hoàn tất và đang chờ admin giải ngân khoản cọc cho người bán."
        );
    }

    void publishProfileRequiredNotification(Payout payout, boolean adminTriggeredReminder) {
        if (payout.getType() == PayoutType.refund) {
            publishOrderNotification(
                    payout.getRecipient().getId(),
                    adminTriggeredReminder
                            ? "Admin nhắc cập nhật tài khoản nhận hoàn tiền"
                            : "Cần cập nhật tài khoản nhận hoàn tiền",
                    adminTriggeredReminder
                            ? "Admin đang chờ bạn cập nhật payout profile để có thể chuyển khoản hoàn tiền."
                            : "Yêu cầu hoàn tiền đã được duyệt nhưng bạn cần cập nhật payout profile để admin chuyển khoản.",
                    "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
            );
            publishAdminPayoutProfileRequiredNotification(
                    payout,
                    "Payout hoàn tiền đang bị chặn vì thiếu payout profile",
                    "Người mua chưa cập nhật payout profile nên admin chưa thể hoàn tiền thủ công."
            );
            return;
        }

        publishOrderNotification(
                payout.getRecipient().getId(),
                adminTriggeredReminder
                        ? "Admin nhắc cập nhật tài khoản nhận giải ngân"
                        : "Cần cập nhật tài khoản nhận giải ngân",
                adminTriggeredReminder
                        ? "Admin đang chờ bạn cập nhật payout profile để có thể giải ngân khoản cọc."
                        : "Đơn hàng đã hoàn tất nhưng bạn cần cập nhật payout profile trước khi nhận khoản cọc.",
                "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
        );
        publishAdminPayoutProfileRequiredNotification(
                payout,
                "Payout giải ngân đang bị chặn vì thiếu payout profile",
                "Người bán chưa cập nhật payout profile nên admin chưa thể giải ngân khoản cọc."
        );
    }

    void publishOrderNotification(UUID userId, String title, String content, String metadata) {
        eventPublisher.publishEvent(new NotificationEvent(
                this,
                userId,
                title,
                content,
                NotificationType.order,
                metadata
        ));
    }

    private void publishAdminPayoutPendingNotification(Payout payout, String title, String content) {
        String metadata = buildPayoutMetadata(payout);

        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        title,
                        content,
                        NotificationType.order,
                        metadata
                )));
    }

    private void publishAdminPayoutProfileRequiredNotification(Payout payout, String title, String content) {
        String metadata = buildPayoutMetadata(payout);

        userRepository.findByRole(AppRole.admin).stream()
                .map(User::getId)
                .distinct()
                .forEach(adminId -> eventPublisher.publishEvent(new NotificationEvent(
                        this,
                        adminId,
                        title,
                        content,
                        NotificationType.order,
                        metadata
                )));
    }

    private String buildPayoutMetadata(Payout payout) {
        return payout.getType() == PayoutType.refund
                ? "{\"refundId\":\"" + payout.getRefundRequest().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}"
                : "{\"orderId\":\"" + payout.getOrder().getId() + "\",\"payoutId\":\"" + payout.getId() + "\"}";
    }
}
