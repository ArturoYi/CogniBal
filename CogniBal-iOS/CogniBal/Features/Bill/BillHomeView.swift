import SwiftUI

struct BillHomeView: View {
    @Environment(\.windowTier) private var tier
    @State private var showingAdd = false

    var body: some View {
        ScrollView {
            VStack(spacing: tier == .compactShort ? 12 : 20) {
                Image(systemName: "book.closed")
                    .font(.system(size: tier == .compactShort ? 28 : 40))
                    .foregroundStyle(.secondary)
                    .accessibilityHidden(true)
                Text("No bills yet")
                    .font(.title3.weight(.semibold))
                Text("New bills show up here.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 24)
            .padding(.top, tier == .compactShort ? 24 : 72)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Ledger")
        .navigationBarTitleDisplayMode(tier == .compactShort ? .inline : .large)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingAdd = true
                } label: {
                    Image(systemName: "plus")
                }
                .accessibilityLabel("Add a bill")
            }
        }
        .sheet(isPresented: $showingAdd) {
            AddBillSheet()
        }
    }
}

struct AccountDetailPlaceholder: View {
    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "creditcard")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("Account")
                .font(.title2.weight(.semibold))
            Text("Account details sit beside the ledger on a wide display.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Account")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AddBillSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var kind: BillKind = .expense

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Add a bill", selection: $kind) {
                        ForEach(BillKind.allCases) { item in
                            Text(item.title).tag(item)
                        }
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
                Section {
                    Text("Saving starts once bill entry is connected.")
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Add a bill")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", role: .cancel) {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}

private enum BillKind: String, CaseIterable, Identifiable {
    case expense
    case income
    case transfer

    var id: String { rawValue }

    var title: LocalizedStringKey {
        switch self {
        case .expense: "Expense"
        case .income: "Income"
        case .transfer: "Transfer"
        }
    }
}
