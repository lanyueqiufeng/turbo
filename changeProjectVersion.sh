read  -p "请输入你要改变后的版本号：" version
if [ "$version" = "" ]
then
    echo "版本为空"
else
    echo "统一改变版本为：$version"
    mvn versions:set -DnewVersion=$version
    read  -p "版本号修改的对吗(y/n)：" submit
    if [ "$submit" = "y" ]
    then
        echo "正在提交版本的修改，修改成功后将删除pom的备份文件"
        mvn versions:commit
    else
        echo "正在撤销版本的修改"
        mvn versions:revert
    fi
fi
# mvn -N versions:update-child-modules