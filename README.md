# Moon Client

For Minecraft Java 26.3 with Fabric Loader 0.20.1 or newer. Install Fabric API for 26.3 alongside the mod. Open Moon Client from the title screen and sign in with a confirmed user from the Moon Client Supabase project's Authentication dashboard.

The mod uses the project's publishable API key and sends credentials over HTTPS to Supabase Auth. Passwords are cleared on submission and never written to disk. Sessions are kept in memory until sign-out, expiration, or game exit.

Build the source with Java 25 and `bash gradlew build`.

For registration, enable Confirm email and use the Supabase Auth Confirm sign up email template with `{{ .Token }}` in the message body. For example, use `<p>Your Moon Client verification code is <strong>{{ .Token }}</strong>.</p>`. Set up custom SMTP for delivery to addresses outside the Supabase organization team.

In Moon Client, use Register to request a code, Verify code to confirm the account, or Resend to request a fresh code. Use code on the login screen to return to verification for a pending account.
