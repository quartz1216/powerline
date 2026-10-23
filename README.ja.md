# Powerline
<p align="center">
  <img src="docs/Header.jpg" alt="Powerline — A command launcher for Android" width="100%">
</p>

## Overview
- Spotlightなどを意識した､Android向けのコマンドランチャー｡
- キーボードファースト - Alt+Spaceで起動｡

## Features
- **アプリ**: アプリ一覧､アプリ名やパッケージ名を含む文字列でのアプリ検索｡
- **計算機**: `12*3`, `200*10%`, `(1+2)/4`. Enter copies the result.
- **WebSearch**: URLクエリでカスタマイズ可能｡エイリアスからアクセス可能｡
- **Share**: 共有のカスタムエイリアスを作成可能｡
- **Cips and Aliases**: Cipsまたはエイリアスから各機能にアクセス可能｡

## How to Use
1. アプリをインストール
2. Powerlineを開きアクセシビリティ権限を許可する
3. 物理キーボードでAlt+Spaceを押下

## If doesn't work
- On OSs with strict task management such as Xiaomi HyperOS, even accessibility services may be automatically killed. You may need to uncheck 'Pause app activity if unused' in App Info or set battery restrictions to 'No restrictions'.

## Support for non-Laten user
- パッケージ名も検索で拾うことで､IME使用ユーザーでも使いやすくしています｡
  - 例えば､アプリ名｢電話｣はパッケージ名｢com.example.phone｣から｢Phone｣で検索可能｡
  - 設定で変更可能｡
- Powerlineを開いたときに自動的にIMEをオフにします｡(オプション｡設定で変更可能｡)
  - 注意:Gboad以外ではテストされていません!
  - 設定で変更可能｡
