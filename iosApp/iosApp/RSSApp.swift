//
//  App.swift
//  iosApp
//
//  Created by Ekaterina.Petrova on 13.11.2020.
//  Copyright © 2020 orgName. All rights reserved.
//

import Foundation
import SwiftUI
import RssReader

@main
class RSSApp: App {
    let rss: RssReader
    let store: ObservableFeedStore
    
    required init() {
        // iOS向けRssReaderの実装を作成する
        rss = RssReader.Companion().create(withLog: true)
        // Swift UI側から監視できるStore
        store = ObservableFeedStore(store: FeedStore(rssReader: rss))
    }
  
    var body: some Scene {
        WindowGroup {
            RootView().environmentObject(store)
        }
    }
}

/**
 ObservableObject プロトコルはApple製。Combineフレームワークの一部
 */
class ObservableFeedStore: ObservableObject {
    
    // @Published ObservableObjectプロトコルに準拠しているクラスにあるフィールドに @Published を付けると
    // 変更をSwift UIに反映することができる
    @Published public var state: FeedState =  FeedState(progress: false, feeds: [], selectedFeed: nil)
    @Published public var sideEffect: FeedSideEffect?
    
    let store: FeedStore
    
    var stateWatcher : Closeable?
    var sideEffectWatcher : Closeable?

    init(store: FeedStore) {
        self.store = store
        // KotlinのFlowはSwiftから使えないので、コールバックに変換している
        stateWatcher = self.store.watchState().watch { [weak self] state in
            self?.state = state
        }
        sideEffectWatcher = self.store.watchSideEffect().watch { [weak self] state in
            self?.sideEffect = state
        }
    }
    
    public func dispatch(_ action: FeedAction) {
        store.dispatch(action: action)
    }
    
    deinit {
        // 監視解除する
        stateWatcher?.close()
        sideEffectWatcher?.close()
    }
}

public typealias DispatchFunction = (FeedAction) -> ()

public protocol ConnectedView: View {
    associatedtype Props
    associatedtype V: View
    
    func map(state: FeedState, dispatch: @escaping DispatchFunction) -> Props
    func body(props: Props) -> V
}

public extension ConnectedView {
    func render(state: FeedState, dispatch: @escaping DispatchFunction) -> V {
        let props = map(state: state, dispatch: dispatch)
        return body(props: props)
    }
    
    var body: StoreConnector<V> {
        return StoreConnector(content: render)
    }
}

public struct StoreConnector<V: View>: View {
    @EnvironmentObject var store: ObservableFeedStore
    let content: (FeedState, @escaping DispatchFunction) -> V
    
    public var body: V {
        return content(store.state, store.dispatch)
    }
}

