import SwiftUI

struct RootView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    @Environment(\.colorScheme) private var colorScheme
    @State private var tab: HomeTab = .bill
    @State private var settingsPage: SettingsDestination?

    private var tier: WindowTier {
        if horizontalSizeClass == .regular {
            return .regular
        }
        if verticalSizeClass == .compact {
            return .compactShort
        }
        return .compact
    }

    var body: some View {
        Group {
            switch tier {
            case .regular:
                RegularShell(tab: $tab, settingsPage: $settingsPage)
            case .compact, .compactShort:
                CompactShell(tab: $tab, tight: tier == .compactShort)
            }
        }
        .environment(\.windowTier, tier)
        .tint(model.preferences.accent.ink(for: colorScheme).primary)
        .preferredColorScheme(model.preferences.colorScheme.scheme)
        .alert(
            "Couldn't open the ledger",
            isPresented: Binding(
                get: { model.storeError != nil },
                set: { isPresented in
                    if !isPresented {
                        model.dismissStoreError()
                    }
                }
            )
        ) {
            Button("OK", role: .cancel) {
                model.dismissStoreError()
            }
        } message: {
            Text(model.storeError?.alertMessage ?? "")
        }
    }
}

struct CompactShell: View {
    @Binding var tab: HomeTab
    var tight: Bool

    var body: some View {
        TabView(selection: $tab) {
            NavigationStack {
                BillHomeView()
            }
            .tabItem {
                Label("Ledger", systemImage: "book.closed")
            }
            .tag(HomeTab.bill)

            NavigationStack {
                SettingsList()
            }
            .tabItem {
                Label("Settings", systemImage: "gearshape")
            }
            .tag(HomeTab.settings)
        }
        .environment(\.windowTier, tight ? .compactShort : .compact)
    }
}

struct RegularShell: View {
    @Binding var tab: HomeTab
    @Binding var settingsPage: SettingsDestination?

    /// 再窄的常规宽度放不下「侧栏 + 账本 + 详情」三列，详情改到更宽的窗口。
    private let detailMinimumWidth: CGFloat = 980

    private var tabSelection: Binding<HomeTab?> {
        Binding(
            get: { tab },
            set: { newValue in
                if let newValue {
                    tab = newValue
                }
            }
        )
    }

    var body: some View {
        GeometryReader { proxy in
            if proxy.size.width >= detailMinimumWidth {
                wide(width: proxy.size.width, height: proxy.size.height)
            } else {
                medium
                    .frame(width: proxy.size.width, height: proxy.size.height)
            }
        }
    }

    /// 侧栏加当前页。iPad 竖屏和 Duo 内屏竖持走这里，标题由系统分栏留出边距。
    private var medium: some View {
        NavigationSplitView {
            tabList
        } detail: {
            NavigationStack {
                primary(showsDetail: false)
            }
        }
        .navigationSplitViewStyle(.balanced)
    }

    private func wide(width: CGFloat, height: CGFloat) -> some View {
        NavigationSplitView {
            tabList
                .navigationSplitViewColumnWidth(min: 200, ideal: 240, max: 280)
        } content: {
            NavigationStack {
                primary(showsDetail: true)
            }
            .navigationSplitViewColumnWidth(min: 320, ideal: 420, max: 560)
        } detail: {
            NavigationStack {
                detail
            }
        }
        .navigationSplitViewStyle(.balanced)
        .frame(width: width, height: height)
    }

    private var tabList: some View {
        List(selection: tabSelection) {
            Label("Ledger", systemImage: "book.closed")
                .tag(HomeTab.bill)
            Label("Settings", systemImage: "gearshape")
                .tag(HomeTab.settings)
        }
        .navigationTitle("CogniBal")
        .listStyle(.sidebar)
    }

    @ViewBuilder
    private func primary(showsDetail: Bool) -> some View {
        switch tab {
        case .bill:
            BillHomeView()
        case .settings:
            if showsDetail {
                SettingsList(selection: $settingsPage)
            } else {
                SettingsList()
            }
        }
    }

    @ViewBuilder
    private var detail: some View {
        switch tab {
        case .bill:
            AccountDetailPlaceholder()
        case .settings:
            if let settingsPage {
                SettingsPage(destination: settingsPage)
            } else {
                SettingsEmptyDetail()
            }
        }
    }
}

struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView()
            .environmentObject(AppModel.preview)
    }
}
