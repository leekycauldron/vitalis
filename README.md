# Vitalis

A heads-up nutrition assistant for Ray-Ban Meta smart glasses. Vitalis streams from the glasses' camera, watches what the wearer is about to eat or drink, and keeps a running food log on the phone. It also offers a menu scanner, a nearby-restaurant finder, and a voice assistant that orchestrates all of it.

Built on Kotlin + Jetpack Compose, the Meta Wearables Device Access Toolkit (MWDAT), Anthropic Claude, ElevenLabs, Google Places, and Google ML Kit.

## Features

### Assistant mode (the default screen once connected)

- **Live first-person stream** from the glasses, with a small overlay card showing the running food log.
- **Passive food logging.** Every 3 seconds Vitalis samples the current frame, gates it through a cheap Haiku "is food in arm's reach?" classifier, and only on YES sends it to a Haiku tool-use call that returns structured `{label, name, calories, protein, carbs, fat, is_junk}` items. Known items are matched to `assets/foods.csv` for canonical macros; unknown items use the model's estimate. Each new entry plays a chime.
- **Anti-duplication** via a normalized per-label cooldown (60 s) plus a "recently logged" hint passed back to the model. So staring at a single coffee for two minutes logs it once, not forty times.
- **Junk-food roast.** If a logged item is flagged as junk for your dietary avoidance, Haiku writes a one-line sarcastic nudge and ElevenLabs speaks it through your phone.
- **Persistent food log** in Room (`vitalis.db`), so totals survive across sessions for future goal tracking and insights.
- **Menu scanner.** When the voice agent says "scan this menu" (or the agent decides to call the tool), a Haiku classifier waits for a frame that clearly shows a menu, then triggers a high-res still capture via the MWDAT SDK. ML Kit runs on-device OCR; Claude Sonnet picks 1–3 items that fit your personal profile. Recommendations are rendered as pulsating dots overlaid on the still, each expanding into a card with a Pexels image and the reason for the pick.
- **Restaurant finder.** "Find me ramen nearby" → grabs the device location via FusedLocationProviderClient, queries the Google Places API (New) Text Search filtered to open restaurants within 10 km, and shows them sorted by travel time. Walk vs. drive is decided by a haversine-distance speed model (walk if ≤1.5 km). Tap a row to open Google Maps directions in the chosen mode.
- **Voice assistant.** A single mic button on the bottom of the assistant screen runs Android `SpeechRecognizer` for STT, sends the transcript + live context (today's macros, your profile, dietary avoidance) to Haiku with tool definitions, and either fires the matching tool (`start_menu_scan`, `find_restaurant`, `summarize_food_log`) or speaks a plain-text reply through ElevenLabs.

### Settings

- Free-text personal profile (goals, weight, height, allergies, preferences).
- Short dietary avoidance field used to flag junk hits and drive roasts.
- API key status rows: Anthropic, Pexels, ElevenLabs (+ voice ID), Google Places.
- Clear-food-log action (with confirmation).

## Architecture overview

```
assistant/         orchestration: AssistantViewModel + state machine
  AnthropicClient            Haiku/Sonnet for menu-scanner pipeline
  AnthropicFoodDetector      Haiku tool-use food logger + scene gate
  RoastGenerator             Haiku roast generation for junk hits
  ElevenLabsClient           TTS
  AudioPlayback              MediaPlayer wrapper (Mutex-serialized)
  MenuOcr                    Google ML Kit on-device OCR
  BitmapEncoding             frame resize + JPEG/base64

voice/             VoiceController + AnthropicAgent + SpeechRecognizerWrapper
foodlog/           Room DB + FoodLogRepository (cooldown, normalization, summarize)
placesearch/       PlacesClient (New API), LocationProvider, TravelEstimator
settings/          DataStore-backed VitalisSettings
stream/            MWDAT camera streaming wrapper (unchanged from the sample base)
ui/                Compose screens + overlays
```

## Prerequisites

- Android Studio Iguana (2023.2.1) or newer
- JDK 17
- Android SDK 35 (compileSdk), targetSdk 34, minSdk 31
- A Ray-Ban Meta device (or use the bundled MockDeviceKit in debug builds)
- Developer Mode enabled in the Meta AI companion app

## Building the app

1. Clone this repository.
2. Open in Android Studio and let Gradle sync.
3. Populate `local.properties` (see below) with at minimum a GitHub token for the MWDAT Maven repo, and any API keys you want the live features to work.
4. Click **Run** > **Run 'app'**.

### `local.properties` keys

```properties
sdk.dir=...                       # set by Android Studio
github_token=ghp_...              # required: read access to facebook/meta-wearables-dat-android Maven

# Optional API keys — each unlocks a corresponding feature; empty values disable it gracefully
ANTHROPIC_API_KEY=sk-ant-...      # food logging, menu scanner, voice assistant, roasts
PEXELS_API_KEY=...                # menu scanner item images
ELEVENLABS_API_KEY=sk_...         # spoken responses + junk roasts
ELEVENLABS_VOICE_ID=...           # voice id from your ElevenLabs library
GOOGLE_PLACES_API_KEY=AIza...     # restaurant finder (Places API New must be enabled in GCP)
```

All keys are read at build time and injected via `BuildConfig`. They never go in source control. The in-app **Settings** screen surfaces a green/red status row per key so you can verify the build picked them up.

## Permissions

Requested in `AndroidManifest.xml` and prompted at runtime:

- `BLUETOOTH`, `BLUETOOTH_CONNECT`, `INTERNET` — MWDAT registration + streaming
- `CAMERA` — wearable camera permission flow
- `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` — restaurant finder
- `RECORD_AUDIO` — voice assistant STT

## Running the app

1. Make sure Developer Mode is on in the Meta AI app and your glasses are paired.
2. Launch Vitalis. On first run, grant Bluetooth + Internet permissions, then tap **Connect** to register.
3. The home screen shows **Start streaming** (the original DAT demo flow) and **Assistant mode**. Tap **Assistant mode**.
4. Once the stream comes up, the food log begins running silently in the background. Look at something edible in front of you to confirm — within a few seconds you should hear the chime and see an entry appear.
5. Tap the mic button to speak. Try:
   - "Scan this menu" while pointing at a menu.
   - "Find me sushi nearby."
   - "How am I doing today?"
   - "What should I avoid at this place?"
6. Open **Settings** (gear icon, top right of assistant mode) to set your profile and dietary avoidance, or to wipe the food log.

## Troubleshooting

- **No food log entries appearing.** Check Logcat tag `Vitalis:AnthropicFoodDetector` — the gate line `Food-scene gate -> 'YES'/'NO'` tells you whether the pre-filter ever passes. If always NO, the gate may be too strict for your scene; if YES but no logs, the tool-use call may be returning an empty array.
- **Duplicate entries for the same item.** Logcat tag `Vitalis:FoodLogRepo` prints `LOGGED ... (key='x')` and `DEDUPED ... (logged Yms ago)` for every detection. If you see two `LOGGED` lines with different normalized keys for the same item, the normalizer probably needs another stopword.
- **Menu scanner returns blurry results.** The pipeline triggers `Stream.capturePhoto()` for a high-res still as soon as Haiku confirms a menu — if you see the still come back blurry, the underlying video frame was already low-quality (check `videoQuality` in `StreamViewModel`).
- **Voice assistant doesn't hear me.** Confirm `RECORD_AUDIO` is granted in system settings, and that Google's speech service is installed on the device (some non-Play ROMs don't ship it).
- **MWDAT-specific errors.** See the [Meta developer documentation](https://wearables.developer.meta.com/docs/develop/) or the [discussions forum](https://github.com/facebook/meta-wearables-dat-android/discussions).

## License

This source code is licensed under the license found in the LICENSE file in the root directory of this source tree.
