const express = require("express");
const crypto = require("crypto");

const app = express();
const TOLERANCE_SECONDS = 300;
let failing = false;
let delayMs = 0;

// Raw body: the signature is computed over the exact bytes HookFlow sent.
app.use(express.raw({ type: "*/*", limit: "1mb" }));

function handler(secret, name) {
  return (req, res) => {
    if (!secret) {
      console.log(name + ": no secret configured");
      return res.status(500).send("no secret configured");
    }

    const timestamp = req.get("X-HookFlow-Timestamp") || "";
    const signature = req.get("X-HookFlow-Signature") || "";
    const body = req.body.toString("utf8");

    // 1. Reject old (or missing) timestamps: this stops replay attacks.
    const age = Math.abs(Date.now() / 1000 - Number(timestamp));
    if (!timestamp || !Number.isFinite(age) || age > TOLERANCE_SECONDS) {
      console.log(name + ": REJECTED stale or missing timestamp");
      return res.status(400).send("stale timestamp");
    }

    // 2. Recompute the signature and compare in constant time.
    const expected = "v1=" + crypto.createHmac("sha256", secret)
      .update(timestamp + "." + body).digest("hex");
    const a = Buffer.from(signature);
    const b = Buffer.from(expected);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
      console.log(name + ": REJECTED bad signature");
      return res.status(401).send("bad signature");
    }

    // 3. Optional failure and slowness simulation for testing retries.
    if (failing) {
      console.log(name + ": SIMULATED FAILURE (500)");
      return res.status(500).send("down");
    }

    console.log(name + ": OK delivery=" + req.get("X-HookFlow-Delivery-Id")
      + " event=" + req.get("X-HookFlow-Event-Id")
      + " type=" + req.get("X-HookFlow-Event-Type") + " body=" + body);
    setTimeout(() => res.status(200).send("ok"), delayMs);
  };
}

app.post("/hooks", handler(process.env.WEBHOOK_SECRET, "/hooks"));
app.post("/hooks2", handler(process.env.WEBHOOK_SECRET_2 || process.env.WEBHOOK_SECRET, "/hooks2"));

app.post("/toggle-fail", (req, res) => {
  failing = !failing;
  console.log("failing =", failing);
  res.json({ failing });
});

app.post("/delay/:ms", (req, res) => {
  delayMs = Number(req.params.ms) || 0;
  console.log("delay =", delayMs, "ms");
  res.json({ delayMs });
});

app.listen(4000, () => console.log("Fake receiver on :4000"));