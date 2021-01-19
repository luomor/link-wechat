import axios from axios
import config from "@/config"

function createAxios(baseUrl) {
  const instance = axios.create({
    baseUrl,
    headers: {
      Auth_token: ""
    },
    timeout: 3000,
  })
  // 请求拦截
  instance.interceptors.request.use(config => {
    // console.log('request config: ' + JSON.stringify(config))
    return config
  }, error => {
    return Promise.reject(error)
  })
  // 响应拦截
  instance.interceptors.response.use(res => {
    // console.log('res: ' + res)
    const { data, status } = res
    // code 0:成功，-1/其它:错误
    if (status === 200 && data.code === 0) {
      return JSON.stringify(data.result) ? data.result : data.data
    } else if (data.code === 201) {
      // Message.error({
      //   content: `产品已存在`,
      //   duration: 3
      // })
    } else {
      addErrorLog(res)
    }
    if (process.env.NODE_ENV === 'development') {
      return Promise.reject(res) // 这样控制台会显示报错日志
    } else {
      return new Promise(() => { }) // 中断promise
    }
  }, error => {
    if (error.response) {
      addErrorLog(error.response)
    } else {
      // Message.error({
      //   content: `服务器未启动或连接超时`,
      //   duration: 3
      // })
      console.error("服务器未启动或连接超时")
    }
    return Promise.reject(error)
  })
  // 错误日志
  const addErrorLog = errorInfo => {
    const { data, statusText, status, request: { responseText, responseURL } } = errorInfo
    // let info = {
    //   type: 'ajax',
    //   code: status,
    //   mes: statusText,
    //   url: responseURL
    // }
    // if (!responseURL.includes('save_error_logger')) store.dispatch('addErrorLog', info)
    process.env.NODE_ENV === 'development'
      ? console.error(`错误: 路径: ${responseURL}, 返回值 : ${responseText}`,)
      : console.error(`${JSON.parse(responseText).message}`,)
  }

  return instance
}

// const httpRequest = createAxios()

export default createAxios(config.domain)