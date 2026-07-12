package org.hameed.hameedmoneycli.constants;

public final class PromptConstants {

    private PromptConstants() {
    }

    public static final String SOURCE_DETECT_PROMPT = """
You are a CSV format detector for a double-entry accounting system. Given a sample of a CSV file, infer the format configuration.

=== THE SCHEMA (the config you must populate) ===

SourceFormatConfig {
  delimiter: String         // e.g. ","  "\\t"  ";"
  hasHeader: boolean        // true if first row is column names
  skipLines: int            // lines to skip before data (0 = start at line 1)
  columns: ColumnMapping[]  // describe each relevant column
  dateFormats: String[]     // date pattern(s) in the date column(s)
  amountPattern: String     // "debit_credit" or "signed"
}

ColumnMapping {
  index: int     // 0-based position in the CSV
  field: String  // "date" | "description" | "amount" | "debit" | "credit"
  name: String   // human-friendly label
}

field descriptions:
  date        — a column containing transaction dates
  description — the narrative, payee, memo, or description of the transaction
  amount      — a single numeric column where positive = inflow, negative = outflow
  debit       — a column with outgoing amounts only
  credit      — a column with incoming amounts only

amountPattern:
  "debit_credit" — when you use separate debit and credit fields
  "signed"       — when you use a single amount field

=== EXAMPLES ===

Example 1 — Simple statement with Date, Description, Amount (signed):
  CSV:
    Date,Description,Amount
    01/06/2026,Grocery Store,-150.00
    03/06/2026,Salary Deposit,5000.00
  Config:
    {"delimiter":",","hasHeader":true,"skipLines":1,"columns":[
      {"index":0,"field":"date","name":"Transaction Date"},
      {"index":1,"field":"description","name":"Description"},
      {"index":2,"field":"amount","name":"Amount"}],
    "dateFormats":["dd/MM/yyyy"],"amountPattern":"signed"}

Example 2 — Statement with Date, Narration, Debit, Credit, Balance:
  CSV:
    Date,Narration,Debit,Credit,Balance
    30-Jun-2026,ATM Withdrawal,1000,,5000
    30-Jun-2026,Interest,,50,5050
  Config:
    {"delimiter":",","hasHeader":true,"skipLines":1,"columns":[
      {"index":0,"field":"date","name":"Date"},
      {"index":1,"field":"description","name":"Narration"},
      {"index":2,"field":"debit","name":"Debit"},
      {"index":3,"field":"credit","name":"Credit"}],
    "dateFormats":["dd-MMM-yyyy"],"amountPattern":"debit_credit"}

Example 3 — Tab-delimited, no header, semicolon decimal:
  CSV:
    2026/06/01;Transfer to Savings;5000
    2026/06/03;Rent payment;-1500,50
  Config:
    {"delimiter":";","hasHeader":false,"skipLines":0,"columns":[
      {"index":0,"field":"date","name":"Date"},
      {"index":1,"field":"description","name":"Description"},
      {"index":2,"field":"amount","name":"Amount"}],
    "dateFormats":["yyyy/MM/dd"],"amountPattern":"signed"}

Now infer the config for this CSV sample:
%s
""";

    public static final String LLM_CLASSIFY_SYSTEM_PROMPT_DEFAULT =
            "You are a personal finance transaction classifier. "
            + "Categorize each bank transaction into the most appropriate account from the provided list. "
            + "Respond with a JSON object: {\"accountName\": \"...\", \"transactionType\": \"BANK_TRANSFER|CARD_TRANSACTION\", \"reasoning\": \"...\"}";

    /**
     * Prompt for converting extracted statement text into a raw CSV.
     * The output CSV preserves the original column layout from the statement
     * (including Balance, Debit, Credit, etc.) so that the format can later
     * be detected by {@link #SOURCE_DETECT_PROMPT} via source update-format.
     * Only normalises layout (multi-line rows → single line), discards metadata.
     */
    public static final String CONVERT_PROMPT = """
You are a financial statement-to-CSV converter. Given the extracted text from a bank statement, extract every transaction row and output a CSV that preserves the original column layout.

=== RULES ===
1. Headers — The first output row must be a header with column names matching the original document (e.g. whatever column names appear in the statement). Do not add, rename, or remove columns.

2. Values — One row per transaction. Preserve all original values exactly (dates, amounts, descriptions) — do not reformat or convert.

3. Quoting — Wrap any field in double quotes if it contains commas. Leave other fields unquoted.

4. Exclusion — Skip summary rows, subtotals, page headers/footers, bank logos, page numbers, blank lines, and metadata. Only output actual transaction rows.

5. Output — ONLY the CSV content. No markdown fences, no explanation.

Extracted text:
%s
""";
}
