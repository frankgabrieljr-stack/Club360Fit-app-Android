# Device Push Notifications Setup

This app sends device push notifications from Supabase Edge Functions whenever app code creates a `client_notifications` row.

## Supabase

Run migration `027_push_device_tokens.sql`, then deploy:

```bash
supabase functions deploy register-device-token --project-ref <project-ref>
supabase functions deploy send-device-push --project-ref <project-ref>
```

Set these Edge Function secrets:

```bash
supabase secrets set FCM_PROJECT_ID="..." --project-ref <project-ref>
supabase secrets set FCM_CLIENT_EMAIL="..." --project-ref <project-ref>
supabase secrets set FCM_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n" --project-ref <project-ref>
supabase secrets set APNS_TEAM_ID="..." --project-ref <project-ref>
supabase secrets set APNS_KEY_ID="..." --project-ref <project-ref>
supabase secrets set APNS_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----" --project-ref <project-ref>
supabase secrets set APNS_BUNDLE_ID="com.frankg.Club360fit" --project-ref <project-ref>
```

## Android

Add Firebase app values to `local.properties`:

```properties
FIREBASE_PROJECT_ID=
FIREBASE_ANDROID_APP_ID=
FIREBASE_API_KEY=
FIREBASE_SENDER_ID=
```

The Android app initializes Firebase manually from those values, registers the FCM token after sign-in, and updates Supabase when Firebase rotates the token.

## iOS

Enable Push Notifications for the iOS app identifier in Apple Developer, and make sure the provisioning profile includes the push entitlement.

The iOS app registers an APNs token natively and stores it through the `register-device-token` Edge Function after sign-in.
