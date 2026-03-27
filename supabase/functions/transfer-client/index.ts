// Deploy: `supabase functions deploy transfer-client --project-ref mjkrokpctcieahxtxvxq`
// Secrets (auto in hosted project): SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY
// Rules:
//  - Caller must be the current coach (clients.coach_id = auth.uid())
//    OR an admin with club360_is_coach_or_admin() = true (for unclaimed clients)
//  - Target coach must exist and have user_metadata.role = 'admin'
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
    // 1. Auth check — require Bearer token
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

    // User client — identifies the caller
    const userClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } },
    });
    const { data: userData, error: userErr } = await userClient.auth.getUser();
    if (userErr || !userData.user) {
      return json(401, { error: "Invalid session" });
    }
    const callerId = userData.user.id;
    const callerRole = userData.user.user_metadata?.role;

    // Caller must be an admin/coach
    if (callerRole !== "admin") {
      return json(403, { error: "Only coach/admin accounts can transfer clients" });
    }

    // 2. Parse body
    const body = (await req.json()) as {
      client_id?: string;
      target_coach_id?: string;
    };
    const clientId = body.client_id?.trim();
    const targetCoachId = body.target_coach_id?.trim();

    if (!clientId) return json(400, { error: "client_id required" });
    if (!targetCoachId) return json(400, { error: "target_coach_id required" });
    if (clientId === targetCoachId) return json(400, { error: "client_id and target_coach_id must differ" });

    // Admin/service client — used for privileged reads and writes
    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { autoRefreshToken: false, persistSession: false },
    });

    // 3. Verify target coach exists and has role = admin
    const { data: targetUser, error: targetErr } = await adminClient.auth.admin.getUserById(targetCoachId);
    if (targetErr || !targetUser?.user) {
      return json(404, { error: "Target coach user not found" });
    }
    if (targetUser.user.user_metadata?.role !== "admin") {
      return json(400, { error: "Target user is not a coach (role must be admin)" });
    }

    // 4. Fetch the client row
    const { data: clientRow, error: clientErr } = await adminClient
      .from("clients")
      .select("id, coach_id")
      .eq("id", clientId)
      .single();

    if (clientErr || !clientRow) {
      return json(404, { error: "Client record not found" });
    }

    // 5. Authorization: caller must be the current coach OR client is unclaimed (coach_id NULL)
    //    Unclaimed clients can be transferred by any admin — but must be claimed first in normal flow.
    //    Here we allow it so admins can reassign orphaned clients.
    const currentCoachId = clientRow.coach_id;
    if (currentCoachId !== null && currentCoachId !== callerId) {
      return json(403, {
        error: "Only the current assigned coach can transfer this client",
      });
    }

    // Prevent self-transfer to same coach
    if (currentCoachId === targetCoachId) {
      return json(400, { error: "Client is already assigned to target coach" });
    }

    // 6. Execute the transfer
    const { error: updateErr } = await adminClient
      .from("clients")
      .update({ coach_id: targetCoachId })
      .eq("id", clientId);

    if (updateErr) {
      return json(500, { error: updateErr.message });
    }

    return json(200, {
      ok: true,
      client_id: clientId,
      previous_coach_id: currentCoachId,
      new_coach_id: targetCoachId,
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
