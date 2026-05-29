/* EB Imprest (SPS) — drives the Input form and renders the two documents
   (Proforma + Expenditure Approval Slip) live, mirroring the workbook's
   formulas. Saves a draft to localStorage and prints each / both as PDF. */
(function () {
  "use strict";

  var STORAGE_KEY = "eb_imprest_sps_v1";

  // Field keys ↔ default sample values (from the source workbook).
  var FIELDS = {
    type: "SPS",
    name: "Thangavelu Street",
    imprest: "1",
    area: "IX",
    depot: "113",
    dae: "26",
    work: "Electricity Consumption Charges for",
    amount: "17258",
    gst: "0",
    period: "01.04.2026 to 15.04.2026",
    contractor: "SE/CEDC/CENTRAL",
    invoice: "           / 2024-25",
    invdate: "15.04.2026",
    code: "2910",
    slipno: ""
  };

  var form = document.getElementById("inputForm");
  var proforma = document.getElementById("proforma");
  var exp = document.getElementById("exp");

  function el(k) { return document.getElementById("f_" + k); }

  // ---- number helpers ------------------------------------------------------
  function n(v) { var x = parseFloat(v); return isNaN(x) ? 0 : x; }
  function num2(x) { return n(x).toFixed(2); }            // Excel "0.00"
  function plain(x) {                                     // number as in text concat
    var v = n(x);
    return Number.isInteger(v) ? String(v) : String(v);
  }

  // ---- read inputs ---------------------------------------------------------
  function read() {
    var d = {};
    Object.keys(FIELDS).forEach(function (k) {
      var e = el(k);
      d[k] = e ? e.value : "";
    });
    return d;
  }

  // ---- derive all document values (mirrors the workbook formulas) ----------
  function derive(d) {
    var area = "Area  " + d.area;
    var section = d.name + " " + d.type + " in Depot " + d.depot;
    var nameOfWork =
      "Expenditure incurred towards the " + d.work + "  " + section +
      " , " + area + " for the period from " + d.period;
    var gstAmtN = n(d.amount) * n(d.gst) / 100;
    var totalN = n(d.amount) + gstAmtN;

    return {
      area: area,
      area2: d.area,
      section: section,
      nameOfWork: nameOfWork,
      period: d.period,
      cost: num2(d.amount),
      contractor: d.contractor,
      reimburse:
        "reimbursement of the above expenditure of Rs. " + plain(d.amount) +
        "  to Contractor,  " + d.contractor,
      ae: "AE  " + d.depot,
      dae: "DAE " + d.dae,
      areaEng: "AREA ENGINEER - " + d.area,
      dept: section + " / " + area,
      codeLabel: "Code: " + d.code,
      gstLabel: "GST (" + (d.gst || "0") + "%)",
      gstAmt: num2(gstAmtN),
      total: num2(totalN),
      say: num2(Math.floor(totalN)),
      payable: "TO WHOME PAYABLE: " + d.contractor,
      slipno: d.slipno
    };
  }

  // ---- paint the [data-o] placeholders in both documents -------------------
  function render() {
    var o = derive(read());
    var nodes = document.querySelectorAll("[data-o]");
    for (var i = 0; i < nodes.length; i++) {
      var key = nodes[i].getAttribute("data-o");
      if (o.hasOwnProperty(key)) nodes[i].textContent = o[key];
    }
  }

  // ---- persistence ---------------------------------------------------------
  function save() {
    try { localStorage.setItem(STORAGE_KEY, JSON.stringify(read())); } catch (e) {}
  }
  function load() {
    var raw;
    try { raw = localStorage.getItem(STORAGE_KEY); } catch (e) { return null; }
    if (!raw) return null;
    try { return JSON.parse(raw); } catch (e) { return null; }
  }
  function setValues(d) {
    Object.keys(FIELDS).forEach(function (k) {
      var e = el(k);
      if (e) e.value = (d && d[k] != null) ? d[k] : FIELDS[k];
    });
  }

  // ---- printing ------------------------------------------------------------
  function printOnly(keep) {
    if (keep !== "proforma") proforma.classList.add("print-hide");
    if (keep !== "exp") exp.classList.add("print-hide");
    window.print();
    proforma.classList.remove("print-hide");
    exp.classList.remove("print-hide");
  }

  // ---- wire up -------------------------------------------------------------
  form.addEventListener("input", function () { render(); save(); });

  document.getElementById("printProforma")
    .addEventListener("click", function () { printOnly("proforma"); });
  document.getElementById("printExp")
    .addEventListener("click", function () { printOnly("exp"); });
  document.getElementById("printBoth")
    .addEventListener("click", function () { window.print(); });

  document.getElementById("saveBtn").addEventListener("click", function () {
    save();
    var b = this; b.textContent = "Saved ✓";
    setTimeout(function () { b.textContent = "Save draft"; }, 1500);
  });
  document.getElementById("clearBtn").addEventListener("click", function () {
    if (!confirm("Reset all fields to the sample values?")) return;
    setValues(FIELDS);
    try { localStorage.removeItem(STORAGE_KEY); } catch (e) {}
    render();
  });

  // ---- init ----------------------------------------------------------------
  setValues(load() || FIELDS);
  render();
})();
