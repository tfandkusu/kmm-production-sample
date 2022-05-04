package com.github.jetbrains.rssreader.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * 状態
 */
interface State

/**
 * アクション
 */
interface Action

/**
 * 副作用
 */
interface Effect

/**
 * ユーザ操作に反応して画面の状態や副作用を管理するクラスのインターフェース
 */
interface Store<S : State, A : Action, E : Effect> {
    /**
     * 画面の状態を取得する
     */
    fun observeState(): StateFlow<S>

    /**
     * 画面の副作用(Toastなど)を取得する
     */
    fun observeSideEffect(): Flow<E>

    /**
     * ユーザ操作などをActionとして渡す
     */
    fun dispatch(action: A)
}