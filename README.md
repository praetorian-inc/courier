<p align="center">
  <img src="assets/courier.png" alt="Courier logo" width="220">
</p>

<h1 align="center">Courier</h1>

<p align="center"><strong>Guard bridge for Burp Suite</strong></p>

Courier connects Burp Suite to Guard for authorized security-testing workflows. It synchronizes selected Burp data, provides a Guard Planner chat, and records reusable browser webflows.

> **Data upload notice:** When connected, Courier uploads captured security-testing data to Guard for active client subscriptions. Uploaded data can include credentials, tokens, cookies, HTTP bodies, audit findings, Organizer items, and recorded webflow values. ML-based training is enabled by default and can be disabled before connecting.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Build from source](#build-from-source)
- [Install in Burp](#install-in-burp)
- [Configure Courier](#configure-courier)
- [Planner workflow](#planner-workflow)
- [Browser webflows](#browser-webflows)
- [Privacy and sensitive data](#privacy-and-sensitive-data)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Features

### Guard connection and synchronization

- Connect to Guard Production or an HTTPS custom endpoint.
- Authenticate with a Guard API key ID and secret.
- Select an authorized tenant and associate data with a project and target application.
- Restrict capture to Burp's target scope and exclude selected file extensions.

### Burp data capture

Courier can synchronize:

- Proxy and other Burp HTTP requests and responses.
- Scanner audit issues and their supporting evidence.
- Organizer items, notes, highlights, and status changes.

### Guard Planner

- Send Burp requests to a Planner queue from the context menu.
- Attach one selected request to the next message in a chat.
- Maintain separate conversations across multiple chat tabs.
- Preview queued requests and responses in Pretty, Raw, or Hex format.

### Browser webflow recording

- Record navigation, clicks, form input, submissions, selections, and keypresses.
- Correlate recorded actions with browser network traffic.
- Save recordings as `.courier` files and synchronize them with Guard.
- Retain entered values so authorized workflows can be repeated.

## Requirements

- Burp Suite Community or Professional.
- An active Guard subscription and Guard API credentials.
- A compatible Chrome or Chromium browser for webflow recording.
- JDK 17 through 26 when building from source.

## Build from source

From the repository root:

```bash
cd burp-extension
./gradlew clean check copyJar
```

The distributable extension is written to:

```text
burp-extension/CourierConnector.jar
```

To generate a versioned platform artifact, SBOM, and checksum:

```bash
./gradlew clean check releaseArtifacts
```

## Install in Burp

1. Open **Extensions → Installed** in Burp Suite.
2. Click **Add**.
3. Choose **Java** as the extension type.
4. Select `burp-extension/CourierConnector.jar`.
5. Confirm that the **Courier** suite tab appears.

## Configure Courier

1. Open the **Courier → Control** tab.
2. Keep **Guard Production** selected, or choose **Custom...** and enter an HTTPS Guard endpoint.
3. Generate an API key ID and secret using the [Guard authentication documentation](https://docs.praetorian.com/articles/25815154096667-getting-started-with-the-praetorian-cli#authentication-to-guard), then enter them in Courier.
4. Optionally refresh and select an authorized tenant.
5. Enter the project name and target application.
6. Review the scope, excluded-extension, ML-training, and logging options.
7. Click **Connect** and review the data-upload disclosure.

Disabling Courier stops capture, synchronization, and active webflow recording.

## Planner workflow

1. Select one or more HTTP messages in Burp.
2. Choose **Send to Courier Planner** from the context menu.
3. Open the **Courier → Planner** tab and select the request to attach.
4. Select the Planner mode and send your message.

Selected evidence is attached once to the next successful message in that chat. Each chat tracks its evidence independently.

## Browser webflows

1. Connect Courier to Guard.
2. Open **Courier → Webflow Recorder**.
3. Click **Record webflow**.
4. Enter a name, project, description, and starting HTTP(S) URL.
5. Complete the workflow in the browser window.
6. Close the page or browser to finish the recording.

Webflow values are intentionally retained for repeatability. Recordings may therefore contain passwords, API keys, session tokens, personal data, and other secrets.

## Privacy and sensitive data

Courier is designed for authorized security testing and can collect highly sensitive data. Before connecting or sharing Courier output:

- Configure Burp's target scope and excluded extensions appropriately.
- Use dedicated testing credentials where possible.
- Review logs and `.courier` recordings before sharing them.
- Protect local Courier files; they are not encrypted.
- Disable ML-based training when captured data must not be used for training.

## Troubleshooting

### Courier does not connect

- Verify the Guard API key ID and secret.
- Confirm that the selected Guard endpoint is reachable.
- Review the Courier activity log and Burp extension output.

### Captured traffic is missing

- Confirm that Courier is connected and enabled.
- Check the Burp target scope setting.
- Review the excluded-extension list.

### Browser recording does not start

- Confirm that Courier is connected.
- Verify that a Burp Proxy listener is running.
- Confirm that Chrome or Chromium can be installed or launched.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for build and validation instructions. Participation is governed by the [Code of Conduct](CODE_OF_CONDUCT.md).

## License

Copyright Praetorian Security Inc.

Courier is licensed under the [Apache License 2.0](LICENSE). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for bundled dependency notices.
