# 初次使用，一个极简的教程
0. 使用前请升级Android Studio到3.0版本，请自行搭梯科学上网（不解释）
1. 打开Android Studio
2. 选择**Check out project from Version Control -> GitHub**
> 如果出现Cannot run program "git.exe" 错误，请[下载](https://git-scm.com/download)安装Git，并重启Android Studio，返回第2步
3. 登陆你的GitHub账号，记得Auth Type选Password
4. 选择**IndoorLocationNG**，然后点Clone
> 如果弹出Github登陆窗，再登陆一遍
5. 开始同步，根据弹出的错误提示安装**Platform**和**build tools**
6. 完成后应该是没有Error的
> 全部的下载过程基于Google，如果过于缓慢或者没进度，请更换更好的梯子😅

# 打不开工程？卡在Gradle Building?
1. 完全退出Android Studio，并[下载Gradle4.1](https://services.gradle.org/distributions/gradle-4.1-all.zip)
2. 断开网络连接
3. 重新打开Android Studio
4. 前往Preferences -> Build, Execution, Deployment -> Gradle.
5. 选择**Use local Gradle distribution**并在**Gradle home**中选择你下载并解压好的Gradle文件夹
6. 重新连接网络，打开工程文件
