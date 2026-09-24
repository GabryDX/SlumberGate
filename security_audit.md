# Application Security Audit Report: SlumberGate
**Date of Audit:** 2026-09-24  
**Audited Target:** Project SlumberGate (Night Lockout & Digital Sunset Engine)  
**Package Identifier:** `com.heronikostudios.slumbergate`  
**Assessment Scope:** Android Application Security Architecture, OWASP Mobile Top 10, IPC Surface, Privacy Safeguards & Misuse Resistance  

---

## 1. Executive Summary

A comprehensive defensive security assessment of the SlumberGate Android codebase was conducted. As an application designed to enforce nighttime digital sunsets via system-level overlays and accessibility guards, the security model was evaluated across five critical domains:
1. **Network Attack Surface & Data Exfiltration**
2. **Inter-Process Communication (IPC) & Component Exposure**
3. **Accessibility Service Capabilities & Privacy Controls**
4. **Life Safety & Emergency Access Guarantees**
5. **Resistance to Stalkerware / Ransomware Misuse**

### Security Scorecard

| Security Domain | Risk Level | Assessment Verdict |
| :--- | :--- | :--- |
| **Network Attack Surface** | **NONE** | **PASS** — Zero network permissions declared; zero sockets or HTTP stacks. |
| **IPC Component Exposure** | **LOW / MINIMAL** | **PASS** — Non-launcher services and receivers are explicitly unexported (`exported="false"`). |
| **Accessibility Privacy** | **LOW** | **PASS** — Configured with `canRetrieveWindowContent="false"`. Cannot inspect keystrokes or text. |
| **Life Safety & Emergency** | **NONE** | **PASS** — Dialer, incoming emergency calls, and morning alarms cannot be suppressed. |
| **Data Protection at Rest** | **LOW** | **PASS** — Sandboxed DataStore; `allowBackup="false"` enforces isolation. |
| **Misuse Resistance** | **LOW** | **PASS** — No Device Admin / MDM locks; accessible emergency unlock gate; uninstallable in Safe Mode. |

**Overall Security Posture:** **STRONG (Defensive Design & Minimal Privilege Scoping)**

---

## 2. Threat Modeling & Abuse Resistance

### 2.1 Threat Scenario 1: Malicious Misuse as Lockware / Ransomware
- **Threat Vector:** An attacker installs the app on a victim's phone to lock them out indefinitely or extort them.
- **Defensive Safeguards:**
  1. **No MDM / Device Admin Authority:** SlumberGate deliberately does **not** request `DevicePolicyManager` lock or device administrator privileges (`BIND_DEVICE_ADMIN`).
  2. **The 60-Second Friction Gate:** The user is never permanently locked out; completing the guided 60-second breathing exercise immediately grants a 3-minute temporary session.
  3. **Daytime Unrestricted Window:** Outside scheduled bedtime hours, the app operates as a normal user app and can be configured, paused, or uninstalled directly from system settings.
  4. **Safe Mode Recovery:** Standard Android Safe Mode disables all third-party accessibility and overlay services without requiring custom recovery or bootloader intervention.

### 2.2 Threat Scenario 2: Emergency Call Obstruction
- **Threat Vector:** A user in an urgent crisis cannot dial emergency services (e.g., 911/112) or receive critical calls during active lockdown.
- **Defensive Safeguards:**
  1. **Dialer Action Bar:** The overlay contains a direct, prominent shortcut to `Intent.ACTION_DIAL`.
  2. **Accessibility Allowlist:** The system default dialer package (`telecomManager.defaultDialerPackage`) and telecom intents are unconditionally exempt from home redirects.
  3. **Incoming Call Suppression:** `SleepWatcherService` monitors `TelephonyManager` states (`CALL_STATE_RINGING`, `CALL_STATE_OFFHOOK`). Upon receiving a ring state, the overlay is immediately hidden so the user can interact with the native call UI.

### 2.3 Threat Scenario 3: Surveillance / Content Scraping via Accessibility
- **Threat Vector:** An accessibility service could potentially read private messages, credentials, or bank details.
- **Defensive Safeguards:**
  1. **`canRetrieveWindowContent="false"`:** Declared in [`accessibility_service_config.xml`](file:///home/trollo/Projects/Android/SlumberGate/app/src/main/res/xml/accessibility_service_config.xml). The Android OS strictly forbids the service from querying accessibility node hierarchies or extracting text from views.
  2. **Event Scoping:** The service only listens for `typeWindowStateChanged` and `typeWindowContentChanged` events to inspect `event.packageName` (string matching), never screen contents or user input.

---

## 3. Component & IPC Exposure Analysis

All components declared in [`AndroidManifest.xml`](file:///home/trollo/Projects/Android/SlumberGate/app/src/main/AndroidManifest.xml) were audited for unauthorized cross-application invocations:

```
┌─────────────────────────────────┬───────────────────┬──────────────────────────────────────┐
│ Component Name                  │ Exported Status   │ Security Protection Mechanism        │
├─────────────────────────────────┼───────────────────┼──────────────────────────────────────┤
│ .MainActivity                   │ exported="true"   │ Protected: Only handles ACTION_MAIN  │
│ .core.service.SleepWatcherService│ exported="false"  │ Fully Isolated: In-app start only    │
│ .core.service.LockdownAccessibilityService │ exported="true" │ Signature Protected: BIND_ACCESSIBILITY_SERVICE │
│ .core.receiver.BootReceiver     │ exported="false"  │ Fully Isolated: System boot only     │
│ .core.receiver.AlarmReceiver    │ exported="false"  │ Fully Isolated: Explicit PendingIntents│
└─────────────────────────────────┴───────────────────┴──────────────────────────────────────┘
```

### Key Findings:
- **Zero Exposed Background Services:** `SleepWatcherService` cannot be commanded or aborted by rogue third-party applications.
- **Protected Accessibility Binding:** Only the Android framework OS can bind to `LockdownAccessibilityService` due to the framework-enforced permission `android.permission.BIND_ACCESSIBILITY_SERVICE`.
- **Broadcast Isolation:** `AlarmReceiver` and `BootReceiver` are `exported="false"`, ensuring third-party apps cannot forge alarm signals or trigger unexpected state transitions.

---

## 4. OWASP Mobile Top 10 Evaluation

| OWASP Mobile Top 10 Category | Assessment Analysis | Status |
| :--- | :--- | :--- |
| **M1: Improper Platform Usage** | Strict adherence to Android 14+ Foreground Service types (`specialUse`) and AlarmManager idle APIs. | **COMPLIANT** |
| **M2: Insecure Data Storage** | Settings stored in app-private sandbox (`DataStore Preferences`). Cloud backup disabled (`allowBackup="false"`). | **COMPLIANT** |
| **M3: Insecure Communication** | N/A — No network calls or transmission mechanisms exist. | **EXEMPT / CLEAN** |
| **M4: Insecure Authentication** | N/A — Offline single-user application without remote credentials. | **EXEMPT / CLEAN** |
| **M5: Insufficient Cryptography**| No custom crypto implemented; relies on platform-level sandboxing. | **COMPLIANT** |
| **M6: Insecure Authorization** | N/A — Local offline state evaluator. | **COMPLIANT** |
| **M7: Client Code Quality** | Kotlin type safety, coroutine lifecycle management with `SupervisorJob`, explicit state machines. | **COMPLIANT** |
| **M8: Code Tampering** | Open-source application; reproducible with standard Gradle toolchains. | **COMPLIANT** |
| **M9: Reverse Engineering** | FOSS codebase designed for full public auditability and transparency. | **COMPLIANT** |
| **M10: Extraneous Functionality**| No hidden debug backdoors, mock endpoints, or developer logging in production code. | **COMPLIANT** |

---

## 5. Permissions & Sensitive Capability Audit

| Permission | Necessity & Security Scope |
| :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | Required to render overlay directly on `WindowManager`. Window params use `PixelFormat.TRANSLUCENT` without deceptive overlays. |
| `BIND_ACCESSIBILITY_SERVICE`| Required to redirect back to home when distracting apps are launched during lockdown. Content reading is explicitly disabled. |
| `ACCESS_FINE_LOCATION` | Required by Android OS starting in API 26+ to read current Wi-Fi SSID. Used exclusively on-device; location coordinates (GPS/Network lat/lon) are never requested or stored. |
| `READ_PHONE_STATE` | Required to detect incoming call ringing status (`CALL_STATE_RINGING`) to hide overlays. Call logs and caller phone numbers are never collected. |
| `POST_NOTIFICATIONS` | Required on Android 13+ (API 33+) to show the ongoing sticky foreground service notification. |
| `SCHEDULE_EXACT_ALARM` | Required on Android 12+ (API 31+) for precise 5-minute wind-down runway alarms. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Whitelists app from aggressive OEM task-killers (e.g. MIUI/EMUI/OneUI memory managers). |

---

## 6. Recommendations & Best Practices for Future Releases

1. **Location Permission Minimization:**
   - *Current Implementation:* Requests `ACCESS_FINE_LOCATION` to inspect `WifiInfo.ssid`.
   - *Recommendation:* When Android 15+ target SDKs evolve, explore companion device pairing or specialized network request APIs if Google further decouples SSID inspection from location permissions.
2. **Tapjacking / Untrusted Touch Safeguards:**
   - On Android 12+, `setHideOverlayWindows(true)` can be called by third-party banking/authenticator apps to hide SlumberGate overlays if their secure screens are active. SlumberGate's whitelist architecture respects system navigation and allows essential utility apps.
3. **Static Analysis & Linting:**
   - Regularly execute `./gradlew lintRelease` to monitor deprecation warnings as target SDKs advance to Android 15+.

---

## 7. Audit Conclusion

Project SlumberGate demonstrates a **mature, privacy-first, defensive security architecture**:
- Physically incapable of data exfiltration due to total absence of network capabilities.
- Appropriately sandboxed with unexported services and signature-guarded accessibility endpoints.
- Designed with robust life-safety fail-safes ensuring emergency telephone and morning alarm availability are never compromised.
