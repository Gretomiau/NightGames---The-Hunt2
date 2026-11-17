package dev.greto.nightminigames.points;

import com.mystrixstudio.points.api.PointsAccess;
import com.mystrixstudio.points.api.PointsApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public final class MystrixPointsBridge {
    private MystrixPointsBridge() {
    }

    public static CompletionStage<TransferResult> grant(String eventId, Map<UUID, Long> amounts) {
        return PointsAccess.get().externalAward("nightminigames", eventId, amounts)
            .thenApply(result -> new TransferResult(result.status() == PointsApi.Status.APPLIED, result.recipients()));
    }

    public record TransferResult(boolean applied, int recipients) {
    }
}
