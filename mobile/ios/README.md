# SENSA iOS app

Native SwiftUI client for the SENSA emergency notification system.

This directory currently contains the **smoke-test scaffold**: a single screen
with the SENSA shield and title. Business logic (authentication, incidents,
file uploads, push notifications) will be added in later iterations.

## Structure

```
mobile/ios/
├── project.yml              # XcodeGen project definition (source of truth)
├── README.md
└── Sources/
    ├── App/
    │   └── SENSAApp.swift   # @main entry point
    └── Views/
        └── ContentView.swift
```

`SENSA.xcodeproj` is **generated** by XcodeGen and is intentionally ignored by
Git. Never edit it by hand: change `project.yml` and regenerate.

## No local Xcode required

The app is built in the cloud on **Codemagic** (macOS runner with Xcode) and
tested in a browser via **Appetize.io**. The repository config is
[`codemagic.yaml`](../../codemagic.yaml) at the repo root.

Local requirements are only an editor (IntelliJ IDEA is fine) and Git. Xcode,
iOS SDK, Simulator and Xcode Command Line Tools are **not** needed locally.

## Codemagic

1. Create an account at <https://codemagic.io> and connect GitHub.
2. Add the `emergency-notification-system` repository as an application.
3. Select the `ios-app` branch. Codemagic detects the root `codemagic.yaml`
   automatically.
4. Start the `sensa-ios-simulator` workflow.
5. The build produces the artifact **`mobile/ios/SENSA-simulator.zip`**
   (a simulator `.app`, not a signed IPA).

Workflow details:

- instance: `mac_mini_m2`, Xcode `16.4` (change to `latest` if that exact
  version is unavailable in the account);
- `xcodegen generate` creates `SENSA.xcodeproj`;
- `xcodebuild` builds for `generic/platform=iOS Simulator` with
  `CODE_SIGNING_ALLOWED=NO`;
- the `.app` is zipped with `ditto` for Appetize.

Triggering is currently limited to the `ios-app` branch, so backend pushes to
`master` do not start iOS builds. Codemagic has no native path filter; after
merging to `master`, either accept builds on every push or add a GitHub Action
that calls the Codemagic API only when `mobile/ios/**` changes.

## Appetize.io

1. Create an account at <https://appetize.io>.
2. Upload `SENSA-simulator.zip`.
3. Select an iPhone simulator and launch the app in the browser.

Optional automation: store the Appetize API token as a **secure environment
variable** in Codemagic (`APPETIZE_API_KEY`) and add an upload step after the
build. Never commit the token to Git.

## Local checks

YAML syntax can be validated without Xcode:

```bash
python3 -c "import yaml; yaml.safe_load(open('../../codemagic.yaml')); yaml.safe_load(open('project.yml')); print('YAML OK')"
```

If `xcodegen` is installed locally (Homebrew: `brew install xcodegen`), the
project can be generated without Xcode:

```bash
cd mobile/ios
xcodegen generate
```

The generated `SENSA.xcodeproj` is ignored by Git; remove it after inspection.

## Adding Swift Package dependencies

Add packages under `packages` in `project.yml` and reference them from the
`SENSA` target:

```yaml
packages:
  SomePackage:
    url: https://github.com/example/SomePackage
    from: 1.0.0

targets:
  SENSA:
    dependencies:
      - package: SomePackage
```

Regenerate the project afterwards. Keep dependencies compatible with iOS 17.

## Troubleshooting

- **`xcodegen: command not found`** — the Codemagic workflow installs it with
  Homebrew; locally install it with `brew install xcodegen`.
- **`xcodebuild: error: scheme SENSA not found`** — regenerate the project and
  check that the scheme in `project.yml` is named `SENSA`.
- **No `.app` in the artifact** — inspect the `Build for iOS Simulator` step;
  the expected path is `build/Build/Products/Debug-iphonesimulator/SENSA.app`.
- **Xcode version unavailable** — change `environment.xcode` in
  `codemagic.yaml` to `latest`.
- **Appetize rejects the ZIP** — make sure the archive contains the `.app`
  bundle at its root (the `ditto --keepParent` command does this).

## Security

Do not commit signing certificates, provisioning profiles, Appetize tokens or
any other secrets. Use Codemagic secure environment variables instead.
