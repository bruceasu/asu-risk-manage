package me.asu.ta.risk.scoring;

import java.time.Instant;
import me.asu.ta.risk.model.RiskWeightProfile;
import me.asu.ta.risk.repository.RiskWeightProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class RiskWeightProfileService {
    public static final String DEFAULT_PROFILE = "DEFAULT";
    public static final String NO_ML_PROFILE = "NO_ML";

    private final RiskWeightProfileRepository repository;

    public RiskWeightProfileService(RiskWeightProfileRepository repository) {
        this.repository = repository;
    }

    public RiskWeightProfile resolveProfile(boolean hasMlSignal) {
        String profileName = hasMlSignal ? DEFAULT_PROFILE : NO_ML_PROFILE;
        return repository.findProfileByName(profileName).orElseGet(() -> fallbackProfile(profileName));
    }

    public RiskWeightProfile findProfileByName(String profileName) {
        return repository.findProfileByName(profileName).orElseGet(() -> fallbackProfile(profileName));
    }

    private RiskWeightProfile fallbackProfile(String profileName) {
        Instant now = Instant.now();
        if (NO_ML_PROFILE.equals(profileName)) {
            return new RiskWeightProfile(NO_ML_PROFILE, 0.55d, 0.30d, 0.0d, 0.15d, true, now, now);
        }
        return new RiskWeightProfile(DEFAULT_PROFILE, 0.40d, 0.25d, 0.20d, 0.15d, true, now, now);
    }
}
