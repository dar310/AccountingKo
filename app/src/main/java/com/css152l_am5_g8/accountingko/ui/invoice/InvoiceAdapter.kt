class InvoiceAdapter(
    private val invoices: List<Invoice>,
    private val onEdit: (Invoice) -> Unit,
    private val onDelete: (Invoice) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.InvoiceViewHolder>() {

    inner class InvoiceViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvoiceViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice_card, parent, false)
        return InvoiceViewHolder(v)
    }

    override fun onBindViewHolder(holder: InvoiceViewHolder, position: Int) {
        val invoice = invoices[position]

        holder.view.findViewById<TextView>(R.id.tv_invoice_name).text = invoice.name
        holder.view.findViewById<TextView>(R.id.tv_invoice_number).text = invoice.number
        holder.view.findViewById<TextView>(R.id.tv_from).text = "From: ${invoice.clientName}"
        holder.view.findViewById<TextView>(R.id.tv_date).text = "Date: ${invoice.date}"
        holder.view.findViewById<TextView>(R.id.tv_due_date).text = "Due: ${invoice.dueDate}"
        holder.view.findViewById<TextView>(R.id.tv_subtotal).text = "₱${invoice.subtotal}"

        holder.view.findViewById<Button>(R.id.btn_edit).setOnClickListener {
            onEdit(invoice)
        }

        holder.view.findViewById<Button>(R.id.btn_delete).setOnClickListener {
            onDelete(invoice)
        }
    }

    override fun getItemCount() = invoices.size
}