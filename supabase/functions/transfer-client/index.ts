// Deploy: `supabase functions deploy transfer-client --project-ref <ref>`
// Secrets (auto in hosted project): SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
// Dashboard (hosted): Edge Functions → transfer-client → Settings → turn OFF "Verify JWT with legacy secret"
//   when this function validates the caller via auth.getUser() itself. If ON, the gateway can return HTTP 401
//   before Deno runs (empty body, no function logs) — often mistaken for an app/session bug.
// Rules:
//  - Caller must be the current assigned coach (clients.coach_id = auth.uid()); unclaimed clients must be claimed first
//  - Target coach must exist and have user_metadata.role = 'admin'
//  - Accepts target_coach_user_id (app) or target_coach_id (alias)
//  - Service role key used ONLY server-side — never exposed to clients
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
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
    const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceRoleKey) {
      return json(500, { error: "SUPABASE_SERVICE_ROLE_KEY not configured" });
    }

    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });

    const { data: userData, error: userErr } = await userClient.auth.getUser();
    if (userErr || !userData.user) {
      return json(401, { error: "Invalid session" });
    }

    const callerId = userData.user.id;
    const callerRole = userData.user.user_metadata?.role;
    if (callerRole !== "admin") {
      return json(403, { error: "Only coach accounts can transfer clients" });
    }

    const body = (await req.json()) as {
      client_id?: string;
      target_coach_user_id?: string;
      target_coach_id?: string;
    };
    const clientId = body.client_id?.trim();
    const rawTarget =
      body.target_coach_user_id?.trim() ?? body.target_coach_id?.trim();
    const targetId = rawTarget?.toLowerCase();

    if (!clientId) {
      return json(400, { error: "client_id required" });
    }
    if (!targetId) {
      return json(400, {
        error: "target_coach_user_id required",
      });
    }
    if (targetId === String(callerId).toLowerCase()) {
      return json(400, { error: "Choose another coach (not yourself)" });
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    const { data: row, error: rowErr } = await adminClient
      .from("clients")
      .select("coach_id")
      .eq("id", clientId)
      .maybeSingle();

    if (rowErr) {
      return json(500, { error: rowErr.message });
    }
    if (!row) {
      return json(404, { error: "Client not found" });
    }

    if (row.coach_id == null) {
      return json(400, {
        error:
          "This member is not assigned to a coach yet. Open their profile once to claim them, then transfer.",
      });
    }

    const assignedCoach = String(row.coach_id).toLowerCase();
    const caller = String(callerId).toLowerCase();
    if (assignedCoach !== caller) {
      return json(403, { error: "Only the assigned coach can transfer this client" });
    }

    const { data: targetUser, error: targetErr } = await adminClient.auth.admin.getUserById(
      targetId,
    );
    if (targetErr || !targetUser?.user) {
      return json(404, { error: "Target coach not found" });
    }

    const targetRole = targetUser.user.user_metadata?.role;
    if (targetRole !== "admin") {
      return json(400, {
        error: "Target user must be a coach account (user_metadata.role = admin)",
      });
    }

    const previousCoachId = row.coach_id;

    const { error: upErr } = await adminClient
      .from("clients")
      .update({ coach_id: targetId })
      .eq("id", clientId);

    if (upErr) {
      return json(500, { error: upErr.message });
    }

    return json(200, {
      ok: true,
      client_id: clientId,
      previous_coach_id: previousCoachId,
      new_coach_id: targetId,
    });
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
