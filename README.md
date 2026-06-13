<div align="center">
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/a8971ec4-3e3a-4133-8665-4cfcc1880d06

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device


---

## 中文翻译

本部分为上文英文说明的中文翻译，已放置在英文版本下面。

<div align="center">
</div>

# 运行并部署您的 AI Studio 应用

本文包含在本地运行该应用所需的一切。

在 AI Studio 中查看您的应用： https://ai.studio/apps/a8971ec4-3e3a-4133-8665-4cfcc1880d06

## 本地运行

**先决条件：**  [Android Studio](https://developer.android.com/studio)


1. 打开 Android Studio
2. 选择 **Open**，然后选择包含此项目的目录
3. 在导入项目时，允许 Android Studio 修复任何不兼容的问题。
4. 在项目目录中创建一个名为 `.env` 的文件，并在该文件中设置 `GEMINI_API_KEY` 为您的 Gemini API 密钥（示例请参见 `.env.example`）
5. 从应用的 `build.gradle.kts` 文件中删除这一行：`signingConfig = signingConfigs.getByName("debugConfig")`
6. 在模拟器或真机上运行该应用
