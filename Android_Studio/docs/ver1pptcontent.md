TECH SPEC IS NOT ACCURATE ANYMORE HAS BEEN UPDATED TO SEEED STUDIO NRF52840 SENSE DEVELOPMENT BOARD

訓練核心痛點
⚫ 據估計，全球有4000萬競技乒乓球運動員，還有數以百萬計的休閒乒乓球愛好
者[1]，在乒乓球訓練中，我們經常面臨這些挑戰，導致技術提升緩慢
缺乏即時反饋
動作完成後難以立即獲
得客觀評價，錯誤姿勢
容易固化
訓練效率低下
重複練習相同錯誤，消
耗大量時間卻進步有限
進步曲線不明顯
缺乏客觀數據，無法量化
技術提升和表現趨勢
1
[1]Olympics.com. (2025). Table Tennis: Olympic history, rules, latest updates and upcoming events for the Olympic sport. Retrieved November 12, 2025, from https://www.olympics.com/en/sports/table-tennis/靈感時刻:讓球拍說話
⚫ 每次揮拍後，我總是在思考：
打了這麼久球，無數次練習後，我不能確定自己的技術是否
有所提升。
⚫ 直到我意識到了關鍵問題：
在揮拍的那一瞬間，我們最需要知道的是「這一拍做得怎麼
樣？」，但沒有即時反饋，只能靠模糊的感覺來判斷。
⚫ 如果球拍能夠「說話」，在我每次揮拍後立即告訴我：
「這一拍角度太小」、「力量不足」、「轉腰不到位」...
那麼我的進步速度會有多快？
2設計概念
「讓每一次揮拍，都有數據相伴；讓每一次學習，都能被看見與改變」
⚫ 設計初衷：
解決傳統乒乓球訓練「缺乏即時數據回饋」的
痛點，讓每一次揮拍都能獲得角度、力度、速
度等資料，幫助使用者及時修正動作，提升學
習速度。
⚫ 核心本質：
強調運動即時數據化與學習可量化、可追蹤的
理念，讓自己與教練都能即時看到進步與問題，
培養持續的訓練循環。
3設計目標與人機協作理念
⚫ 即時量化反饋
■ 每次揮拍後立即提供反饋，確
保動作記憶最佳矯正時機
SmartRacket 的設計目標是打造人
機協作的訓練輔助系統
⚫ 輔助而非替代
◼ 強化教練與學員互動，AI提供
客觀數據給教練分析
Reference: [https://www.youtube.com/watch?v=j8hQ66LXutc](https://www.youtube.com/watch?v=j8hQ66LXutc)
4硬體演進: Micro:bit → ESP32
從原型驗證到效能優化的硬體迭
[https://youtu.be/twZvTk2d-PM](https://youtu.be/twZvTk2d-PM)
5硬體演進: Micro:bit → ESP32
現有 MVP 的主要限制
⚫ 體積過大
⚫ 續航力弱
⚫ 運算能力有限
ESP32 + IMU 解決方案
⚫ 不影響握拍感受，無縫整合於球拍手柄
⚫ 長效續航 低功耗設計
⚫ 透過手機實現即時動作分析和智能回饋
6技術出發點與動機
讓訓練輔助工具不再遙不可及
⚫ 成本控制
◼ 採用ESP32開發版及IMU傳感器，單套硬體
成本約一百多港元
⚫ 混合邊緣計算架構
◼ 邊緣端(球拍)收集數據 → 移動端(手機)運行
ai模型及提供反饋，將資源利用最佳化
⚫ 全場景適用
◼ 離線運作：無需互聯網連接，室內外任何場
地皆可使用
◼ 環境獨立：不依賴光線、背景、場地標記等
外部條件
7設計可行性分析
⚫ 在網絡上已有方案實現了一個完整的 AI 智能乒
乓球拍原型[1]，證明了我們 SmartRacket 的概念
在技術上是完全可實現的。
⚫ 影片中的設計方案
◼ 採用Arduino Nano 33 BLE Sense微控制器、陀
螺儀、及TensorFlow Micro（結合Google Tiny
Motion Trainer）
◼ 3D列印TPU拍柄外殼，小型鋰電池
◼ 使用藍牙進行數據收集與分類模型訓練
⚫ 技術可行性
◼ 成功分類多種乒乓波擊球動作（如正手上旋、擋
球、削球等）。
[1]Alexander, S. (2021, July 19). Here’s why I made the smartest table tennis bat with AI [Video]. YouTube. [https://www.youtube.com/watch?v=j8hQ66LXutcAndroid](https://www.youtube.com/watch?v=j8hQ66LXutcAndroid) 原生應用(Samsung優化)
為三星設備精心優化的高效能、低延遲使用體驗
SmartRacket Coach
即時反饋系統
透過ML動作分類，實時識別球拍動作，並提供1-10分評分與
針對性建議。
即時反饋
8.2
正手攻 | 速度良好 | 角度穩定
訓練歷史追蹤
自動記錄每次訓練的動作分佈、評分與持續時間，生成訓練日
誌，清晰呈現進步曲線。
即時數據圖表
數據分析引擎
運用輕量級ML模型在本地進行動作模式識別，提供個性化訓練
建議。
改進建議
• 手腕更加放鬆，提高揮拍流暢度
• 接觸點略微提前，改善擊球穩定性
主頁 歷史 分析 設置
Samsung生態整合
可整合Galaxy Watch，實現心率與運動數據協同分析。
9Galaxy Watch整合 融合智能手錶 ， 創造全方位運動健康體驗
即時心率監測
結合運動強度與心率數據，提供精確建議，最大化訓練效果。
心率監測
136
次/分鐘
訓練狀態推送
實時將球拍數據分析結果推送至手腕，無需查看手機即可即時掌
握動作技術改進建議。
健康數據同步
自動整合Samsung Health數據，提供全方位健康視圖，包含
卡路里消耗、活動量與恢復狀態分析。
透過Galaxy生態整合，SmartRacket 為用戶提供跨設備體驗，同時
為三星裝置帶來獨特的競爭優勢。
10高光一刻功能介紹
革命性的片段捕捉體驗，讓每個精彩瞬間不再錯過
靈感來源
靈感源自NVIDIA Instant Replay遊戲錄製技術
將其應用於運動領域
一鍵保存
擊球精彩時只需輕觸按鈕，系統自動保存、整理並標記精彩片段
循環錄製
自動錄製最近10分鐘畫面，確保每個精彩瞬間都能被捕捉
高效率分享
節省影片編輯時間及手機儲存空間，短時間內完成精彩片段分享
Reference:
[https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExNW9ka3VpNmIxMzY4aTE5NTh](https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExNW9ka3VpNmIxMzY4aTE5NTh)
pYXp5Mzh6NmtjMzR2aWNvanhlcHFzZyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3
Q9Zw/7GtdMQAyHSR1K/giphy.gif
11高光一刻×三星生態價值
Reference: https://www.samsung.com/us/support/owners/app/samsung-
health
Reference: [https://www.samsung.com/hk_en/support/model/SMART-THINGS/](https://www.samsung.com/hk_en/support/model/SMART-THINGS/)
Watch推送
將訓練中的精彩瞬間自動推送至Galaxy Watch，震動提醒
智慧家庭
備進行互動
透過SmartThings自動化場景，訓練高光時刻可觸發家中智能設
健康數據
與Samsung Health整合，結合心率、消耗卡路里等生理數據進
行分析
12使用者影響
13社會層面影響
⚫ 人才培育
∵業餘愛好者缺乏系統化數據指導
透過將「專業級數據分析」普及
◼ 縮短技術突破週期，提升香港乒乓球國際競爭力
⚫ 推廣健康運動風氣
∵業餘愛好者缺乏訓練指導，容易在訓練/遊玩時受傷
透過推廣SmartRacket，指出不正確的姿勢
◼ 及時修正 →降低運動傷害風險
14應用場景擴展:從球拍到更多
⚫ 其他球拍運動
適用於擁有相似揮拍機制的球拍類運動，
E.G. 網球,羽毛球,壁球
⚫ 揮拍類運動
具有類似揮拍動作的其他運動項目
E.G.棒球,高爾夫,板球
⚫ 健康監測應用
未來可拓展至健康與醫療領域的應用場景
E.G.復健訓練, 姿勢矯正
Reference:https://www.thefarmersmarketgl
obal.com/products/tennis-racket
Reference:https://www.reddit.com/r/10s/comm
ents/1jxhbvo/how_to_make_yonex_grip_butt_c
ap_feel_more_like/