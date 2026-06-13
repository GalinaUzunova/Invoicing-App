(function () {
    const lineItems = document.querySelector("[data-line-items]");
    const template = document.getElementById("line-item-template");
    const addButton = document.getElementById("add-line-item");
    const invoicePreview = document.querySelector("[data-invoice-preview]");

    if (!lineItems || !template || !addButton) {
        return;
    }

    function parseMoney(value) {
        const parsed = Number.parseFloat(value);
        return Number.isFinite(parsed) ? parsed : 0;
    }

    function formatMoney(value) {
        return value.toFixed(2);
    }

    function calculateRow(row) {
        const quantity = parseMoney(row.querySelector("[data-quantity]")?.value);
        const unitPrice = parseMoney(row.querySelector("[data-unit-price]")?.value);
        const taxRate = parseMoney(row.querySelector("[data-tax-rate]")?.value);
        const subtotal = quantity * unitPrice;
        const taxAmount = subtotal * taxRate / 100;
        const total = subtotal + taxAmount;
        const preview = row.querySelector("[data-line-preview]");

        if (preview) {
            preview.textContent = formatMoney(total);
        }

        return total;
    }

    function recalculatePreview() {
        const total = Array.from(lineItems.querySelectorAll("[data-line-item-row]"))
            .reduce((sum, row) => sum + calculateRow(row), 0);

        if (invoicePreview) {
            invoicePreview.textContent = formatMoney(total);
        }
    }

    function reindexRows() {
        Array.from(lineItems.querySelectorAll("[data-line-item-row]")).forEach((row, index) => {
            row.querySelectorAll("[name]").forEach((input) => {
                input.name = input.name.replace(/lineItems\[\d+]/, `lineItems[${index}]`);
            });
        });
    }

    function addRow() {
        const index = lineItems.querySelectorAll("[data-line-item-row]").length;
        const html = template.innerHTML.replaceAll("__index__", String(index));
        lineItems.insertAdjacentHTML("beforeend", html);
        recalculatePreview();
    }

    addButton.addEventListener("click", addRow);

    lineItems.addEventListener("input", (event) => {
        if (event.target.matches("[data-quantity], [data-unit-price], [data-tax-rate]")) {
            recalculatePreview();
        }
    });

    lineItems.addEventListener("click", (event) => {
        if (!event.target.matches("[data-remove-line-item]")) {
            return;
        }

        const rows = lineItems.querySelectorAll("[data-line-item-row]");
        if (rows.length <= 1) {
            return;
        }

        event.target.closest("[data-line-item-row]").remove();
        reindexRows();
        recalculatePreview();
    });

    recalculatePreview();
})();
