# recode-useful-tools 3.2.0 — デコンパイル済みソース

アップロードされた `recode-useful-tools-3.2.0.jar` を静的に解析し、CFR 0.153-SNAPSHOTでJavaソースへ復元したものです。元のJARは実行していません。

## 内容

- `src/main/java/` — 復元されたJavaソース（112ファイル）
- `src/main/resources/` — Fabric設定、Mixin設定、シェーダー、アイコン、同梱JARなどの非クラスリソース
- `reports/decompilation-summary.txt` — 件数とハッシュ
- `reports/cfr-processing.log` — CFRの処理ログ
- `reports/jar-entries.txt` — 元JARの収録エントリ一覧

元JARには212個のclassファイルがあり、トップレベル112個、内部・匿名クラス100個でした。内部・匿名クラスは対応するトップレベルのJavaファイルへ統合されているため、生成Javaファイル数は112です。CFRがメソッド本体を復元できなかったことを示すマーカーは検出されませんでした。

## 注意事項

デコンパイル結果は元のソースコードと完全一致しません。コメント、空白、ローカル変数名、ジェネリクス表現、ラムダ式などが元と異なる場合があります。

Minecraft、Fabric API、Mixin、ImGuiなどの外部依存関係はソースとして含まれていないため、そのままではコンパイルできない可能性があります。また、配布用アーカイブには同梱フォント資産を含めていません。
