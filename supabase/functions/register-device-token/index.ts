// Deploy: `supabase functions deploy register-device-token --project-ref <ref>`
// Secrets (auto in hosted project): SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

type RegisterDeviceTokenBody = {
  platform?: "android_fcm" | "ios_apns";
  token?: string;
  environment?: "sandbox" | "production";
  app_version?: string;
  device_id?: string;
  enabled?: boolean;
};

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: cors });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader?.startsWith("Bearer ")) {
      return json(401, { error: "Missing Authorization" });
    }

    const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
    const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceRoleKey) {
      return json(500, { error: "SUPABASE_SERVICE_ROLE_KEY not configured" });
    }

    const userClient = createClient(supabaseUrl, anonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await userClient.auth.getUser();
    if (userErr || !userData.user) {
      return json(401, { error: "Invalid session" });
    }

    const body = (await req.json()) as RegisterDeviceTokenBody;
    const platform = body.platform;
    const token = body.token?.trim();
    const environment = body.environment ?? "production";

    if (platform !== "android_fcm" && platform !== "ios_apns") {
      return json(400, { error: "platform must be android_fcm or ios_apns" });
    }
    if (!token) {
      return json(400, { error: "token required" });
    }
    if (environment !== "sandbox" && environment !== "production") {
      return json(400, { error: "environment must be sandbox or production" });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data, error } = await adminClient
      .from("push_device_tokens")
      .upsert(
        {
          user_id: userData.user.id,
          platform,
          token,
          environment,
          app_version: body.app_version ?? null,
          device_id: body.device_id ?? null,
          enabled: body.enabled ?? true,
          last_seen_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        },
        { onConflict: "platform,token" },
      )
      .select("id")
      .single();

    if (error) {
      return json(500, { error: error.message });
    }

    return json(200, { ok: true, id: data?.id });
  } catch (e) {
    return json(500, { error: String(e) });
  }
});

function json(status: number, body: Record<string, unknown>) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}
