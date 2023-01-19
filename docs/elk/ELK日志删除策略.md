# 设置模板：

PUT _template/elk_template
{
"index_patterns": ["cloud-healht-wx-*"],                 
"settings": {
"index":{
"lifecycle":{
"name":"auto_delete_policy",
"indexing_complete":true
}
}

}
}

# 设置策略：

PUT _ilm/policy/auto-delete-org-policy   
{
"policy": {
"phases": {
"delete": {
"min_age": "7d",
"actions": {
"delete": {}
}
}
}
}
}

# 查看策略：

GET _ilm/policy/

GET consult-service-dev-*/_ilm/explain

# 生命周期策略默认10分钟检测一次

PUT /_cluster/settings
{
"transient": {
"indices.lifecycle.poll_interval":"60s"
}
}