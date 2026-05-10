# Play Console form answers

Pre-baked answers for the questionnaires Google asks before publishing.

---

## Data safety

Section: **Policy → App content → Data safety**

### Does your app collect or share any of the required user data types?

> **No**

(BPlayer reads files from a folder you grant via SAF and stores playback
position locally. No data is sent off-device, no third-party SDKs, no
analytics. Nothing in Google's "user data" categories applies.)

### Is all of the user data collected by your app encrypted in transit?

> Not applicable — no data leaves the device.

(Google's form may force you to answer yes or no. Pick "Yes" — there's no
data, but no data trivially satisfies "encrypted in transit.")

### Do you provide a way for users to request that their data is deleted?

> Not applicable — nothing is collected.

---

## Content rating

Section: **Policy → App content → Content rating**

The IARC questionnaire asks about content categories. For BPlayer:

| Category                    | Answer | Reason                              |
| --------------------------- | ------ | ----------------------------------- |
| Violence                    | No     | None.                               |
| Sexuality                   | No     | None.                               |
| Profanity                   | No     | None.                               |
| Controlled substances       | No     | None.                               |
| Gambling                    | No     | None.                               |
| Crude humor                 | No     | None.                               |
| User-generated content      | No     | App plays user's local audio files only. |
| Sharing of user location    | No     | None.                               |
| Personal information shared | No     | None.                               |
| Web browsing                | No     | No browser, no network.             |
| Digital purchases           | No     | None.                               |

Likely outcome: rated **Everyone / 3+** in every region.

---

## Target audience and content

Section: **Policy → App content → Target audience**

> Target age range: **18 and over** (or 13+ if preferred).

(Choosing 18+ avoids the extra hoops Google adds for apps that target
children — designed-for-families program, COPPA disclosures, etc.
BPlayer isn't meant for kids specifically, so 18+ keeps it simple.)

> Is your app appealing to children? **No**

---

## Ads

Section: **Policy → App content → Ads**

> Does your app contain ads? **No**

---

## Government apps

> No.

---

## Health features

> No.

---

## Permissions and APIs

Section: **Policy → App content → Permissions**

BPlayer uses these declared permissions; none are sensitive Play Console
permissions (no SMS, no location, no contacts, no all-files-access). No
declaration form needed.

| Permission                            | Why                                                 |
| ------------------------------------- | --------------------------------------------------- |
| FOREGROUND_SERVICE                    | Audio playback continues with screen off.           |
| FOREGROUND_SERVICE_MEDIA_PLAYBACK     | Foreground service type required by Android 14+.   |
| POST_NOTIFICATIONS                    | Show the lock-screen playback notification.         |
| WAKE_LOCK                             | Keep CPU running during playback.                   |

---

## App access

> All of the app's functionality is available without any access restrictions.

(No login, no payment, no special unlock.)

---

## Government information

> No.

---

## News app

> No.

---

## COVID-19 contact tracing and status apps

> No.

---

## Pricing and distribution

- Country availability: **all countries** (or restrict if you prefer).
- Price: **Free**.
- In-app purchases: **None**.
