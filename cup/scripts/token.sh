kubectl get secret admin-user -n kubernetes-dashboard -o jsonpath={".data.token"} | base64 -d


#kubectl -n kubernetes-dashboard create token admin-user