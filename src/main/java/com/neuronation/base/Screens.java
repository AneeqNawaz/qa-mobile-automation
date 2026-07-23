package com.neuronation.base;

import com.neuronation.pages.common.*;
import com.neuronation.pages.med.registration.*;
import com.neuronation.pages.med.onboarding.*;
import com.neuronation.pages.med.profile.*;
import com.neuronation.pages.med.extras.*;

/**
 * Playwright-style fixture container for all screen page objects.
 * Screens are lazy-initialized on first access — no wasted objects.
 *
 * Usage in any test:
 *   screens.launch().tapStartNow();
 *   screens.dashboard().tapProfileTab();
 *   assertTrue(screens.profile().isProfileDisplayed());
 */
public class Screens {

    // === Common ===
    private LaunchScreen launch;
    private DashboardScreen dashboard;
    private ProfileScreen profile;
    private SettingsScreen settings;
    private PrivacySettingsScreen privacySettings;
    private ConsentHistoryScreen consentHistory;
    private TrainingReminderScreen trainingReminder;
    private ChangeEmailScreen changeEmail;
    private LoginScreen login;

    // === MED — Registration flow (screens 1-10) ===
    private AppSelectionScreen appSelection;
    private DiGACodeScreen digaCode;
    private OnboardingVideoScreen onboardingVideo;
    private CreateAccountScreen createAccount;
    private EmailRegistrationScreen emailRegistration;
    private PasskeyScreen passkey;
    private SetPasswordScreen setPassword;
    private EmailVerificationScreen emailVerification;
    private DoctorInfoScreen doctorInfo;

    // === MED — Onboarding flow (screens 11-30) ===
    private NewsletterConsentScreen newsletterConsent;
    private TipsScreen tips;
    private ExerciseIntroScreen exerciseIntro;
    private ExerciseInGameScreen exerciseInGame;
    private AgeGroupScreen ageGroup;
    private EvaluationScreen evaluation;
    private TrainingComplexityScreen trainingComplexity;
    private SpecialNeedsScreen specialNeeds;
    private TrainingTimeScreen trainingTime;
    private ScheduleReviewScreen scheduleReview;
    private NeuroBoosterScreen neuroBooster;
    private PromiseScreen promise;

    // === MED — Extras (Neurobooster) ===
    private ExtrasScreen extras;
    private NeuroBoosterDetailScreen neuroBoosterDetail;
    private NeuroBoosterVideoScreen neuroBoosterVideo;
    private NeuroBoosterQuizScreen neuroBoosterQuiz;
    private NeuroBoosterQuizResultScreen neuroBoosterQuizResult;

    // --- Common ---
    public LaunchScreen launch() {
        if (launch == null) launch = new LaunchScreen();
        return launch;
    }

    public DashboardScreen dashboard() {
        if (dashboard == null) dashboard = new DashboardScreen();
        return dashboard;
    }

    public ProfileScreen profile() {
        if (profile == null) profile = new ProfileScreen();
        return profile;
    }

    public SettingsScreen settings() {
        if (settings == null) settings = new SettingsScreen();
        return settings;
    }

    public PrivacySettingsScreen privacySettings() {
        if (privacySettings == null) privacySettings = new PrivacySettingsScreen();
        return privacySettings;
    }

    public ConsentHistoryScreen consentHistory() {
        if (consentHistory == null) consentHistory = new ConsentHistoryScreen();
        return consentHistory;
    }

    public TrainingReminderScreen trainingReminder() {
        if (trainingReminder == null) trainingReminder = new TrainingReminderScreen();
        return trainingReminder;
    }

    public ChangeEmailScreen changeEmail() {
        if (changeEmail == null) changeEmail = new ChangeEmailScreen();
        return changeEmail;
    }

    public LoginScreen login() {
        if (login == null) login = new LoginScreen();
        return login;
    }

    // --- MED Registration flow ---
    public AppSelectionScreen appSelection() {
        if (appSelection == null) appSelection = new AppSelectionScreen();
        return appSelection;
    }

    public DiGACodeScreen digaCode() {
        if (digaCode == null) digaCode = new DiGACodeScreen();
        return digaCode;
    }

    public OnboardingVideoScreen onboardingVideo() {
        if (onboardingVideo == null) onboardingVideo = new OnboardingVideoScreen();
        return onboardingVideo;
    }

    public CreateAccountScreen createAccount() {
        if (createAccount == null) createAccount = new CreateAccountScreen();
        return createAccount;
    }

    public EmailRegistrationScreen emailRegistration() {
        if (emailRegistration == null) emailRegistration = new EmailRegistrationScreen();
        return emailRegistration;
    }

    public PasskeyScreen passkey() {
        if (passkey == null) passkey = new PasskeyScreen();
        return passkey;
    }

    /** @deprecated use {@link #passkey()} — same screen, renamed to PasskeyScreen and moved to profile package. */
    @Deprecated
    public PasskeyScreen passkeyDialog() { return passkey(); }

    public SetPasswordScreen setPassword() {
        if (setPassword == null) setPassword = new SetPasswordScreen();
        return setPassword;
    }

    public EmailVerificationScreen emailVerification() {
        if (emailVerification == null) emailVerification = new EmailVerificationScreen();
        return emailVerification;
    }

    public DoctorInfoScreen doctorInfo() {
        if (doctorInfo == null) doctorInfo = new DoctorInfoScreen();
        return doctorInfo;
    }

    // --- MED Onboarding flow ---
    public NewsletterConsentScreen newsletterConsent() {
        if (newsletterConsent == null) newsletterConsent = new NewsletterConsentScreen();
        return newsletterConsent;
    }

    public TipsScreen tips() {
        if (tips == null) tips = new TipsScreen();
        return tips;
    }

    public ExerciseIntroScreen exerciseIntro() {
        if (exerciseIntro == null) exerciseIntro = new ExerciseIntroScreen();
        return exerciseIntro;
    }

    public ExerciseInGameScreen exerciseInGame() {
        if (exerciseInGame == null) exerciseInGame = new ExerciseInGameScreen();
        return exerciseInGame;
    }

    public AgeGroupScreen ageGroup() {
        if (ageGroup == null) ageGroup = new AgeGroupScreen();
        return ageGroup;
    }

    public EvaluationScreen evaluation() {
        if (evaluation == null) evaluation = new EvaluationScreen();
        return evaluation;
    }

    public TrainingComplexityScreen trainingComplexity() {
        if (trainingComplexity == null) trainingComplexity = new TrainingComplexityScreen();
        return trainingComplexity;
    }

    public SpecialNeedsScreen specialNeeds() {
        if (specialNeeds == null) specialNeeds = new SpecialNeedsScreen();
        return specialNeeds;
    }

    public TrainingTimeScreen trainingTime() {
        if (trainingTime == null) trainingTime = new TrainingTimeScreen();
        return trainingTime;
    }

    public ScheduleReviewScreen scheduleReview() {
        if (scheduleReview == null) scheduleReview = new ScheduleReviewScreen();
        return scheduleReview;
    }

    public NeuroBoosterScreen neuroBooster() {
        if (neuroBooster == null) neuroBooster = new NeuroBoosterScreen();
        return neuroBooster;
    }

    public PromiseScreen promise() {
        if (promise == null) promise = new PromiseScreen();
        return promise;
    }

    // --- MED Extras (Neurobooster) ---
    public ExtrasScreen extras() {
        if (extras == null) extras = new ExtrasScreen();
        return extras;
    }

    public NeuroBoosterDetailScreen neuroBoosterDetail() {
        if (neuroBoosterDetail == null) neuroBoosterDetail = new NeuroBoosterDetailScreen();
        return neuroBoosterDetail;
    }

    public NeuroBoosterVideoScreen neuroBoosterVideo() {
        if (neuroBoosterVideo == null) neuroBoosterVideo = new NeuroBoosterVideoScreen();
        return neuroBoosterVideo;
    }

    public NeuroBoosterQuizScreen neuroBoosterQuiz() {
        if (neuroBoosterQuiz == null) neuroBoosterQuiz = new NeuroBoosterQuizScreen();
        return neuroBoosterQuiz;
    }

    public NeuroBoosterQuizResultScreen neuroBoosterQuizResult() {
        if (neuroBoosterQuizResult == null) neuroBoosterQuizResult = new NeuroBoosterQuizResultScreen();
        return neuroBoosterQuizResult;
    }

    /**
     * Reset all cached screens. Called between tests so stale driver references don't leak.
     */
    public void reset() {
        launch = null; dashboard = null; profile = null; settings = null;
        privacySettings = null; consentHistory = null;
        trainingReminder = null; changeEmail = null; login = null;
        appSelection = null; digaCode = null; onboardingVideo = null;
        createAccount = null; emailRegistration = null; passkey = null;
        setPassword = null; emailVerification = null; doctorInfo = null;
        newsletterConsent = null; tips = null; exerciseIntro = null;
        exerciseInGame = null; ageGroup = null; evaluation = null;
        trainingComplexity = null; specialNeeds = null; trainingTime = null;
        scheduleReview = null; neuroBooster = null; promise = null;
        extras = null; neuroBoosterDetail = null; neuroBoosterVideo = null;
        neuroBoosterQuiz = null; neuroBoosterQuizResult = null;
    }
}
