package skinny.controller

import skinny._
import skinny.orm.ParamType
import skinny.validator.MapValidator
import skinny.exception.StrongParametersException

trait SkinnyResource extends SkinnyController {

  protected def skinnyCRUDMapper: SkinnyCRUDMapper[_]
  protected def resourcesName: String
  protected def resourceName: String

  protected def responseFormats: Seq[Format] = Seq(Format.HTML, Format.JSON, Format.XML)
  protected def withFormat[A](format: Format)(action: => A): A = {
    responseFormats.find(_ == format) getOrElse haltWithBody(406)
    action
  }

  protected def useRelativePath: Boolean = false
  protected def basePath: String = if (useRelativePath) "" else s"/${resourcesName}"

  def showResources()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    set(resourcesName, skinnyCRUDMapper.findAll())
    render(s"/${resourcesName}/index")
  }

  def showResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    set(resourceName, skinnyCRUDMapper.findById(id).getOrElse(haltWithBody(404)))
    render(s"/${resourcesName}/show")
  }

  def newResource()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    render(s"/${resourcesName}/new")
  }

  protected def createForm: MapValidator
  protected def createFormStrongParameters: Seq[(String, ParamType)]

  protected def doCreateAndReturnId(parameters: PermittedStrongParameters) = {
    logger.debug(s"Permitted parameters: ${parameters.params.mkString("[", ",", "]")}")
    skinnyCRUDMapper.createWithAttributes(parameters)
  }

  def createResource()(implicit format: Format = Format.HTML): Any = withFormat(format) {
    if (createForm.validate()) {
      val id: Long = if (!createFormStrongParameters.isEmpty) {
        val parameters = params.permit(createFormStrongParameters: _*)
        doCreateAndReturnId(parameters)
      } else {
        throw new StrongParametersException(
          "'createFormStrongParameters' or 'createFormTypedStrongParameters' must be defined.")
      }
      status = 201
      // TODO path helper
      response.setHeader("Location", s"${contextPath}/${resourcesName}/${id}")
      format match {
        case Format.HTML =>
          set(resourceName, skinnyCRUDMapper.findById(id).getOrElse(haltWithBody(404)))
          render(s"/${resourcesName}/show")
        case _ =>
      }
    } else {
      render(s"/${resourcesName}/new")
    }
  }

  def editResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    skinnyCRUDMapper.findById(id).map {
      m =>
        status = 200
        format match {
          case Format.HTML =>
            setAsParams(m)
            render(s"/${resourcesName}/edit")
          case _ =>
        }
    } getOrElse haltWithBody(404)
  }

  protected def updateForm: MapValidator
  protected def updateFormStrongParameters: Seq[(String, ParamType)]

  protected def doUpdate(id: Long, parameters: PermittedStrongParameters): Unit = {
    logger.debug(s"Id: ${id}, Permitted parameters: ${parameters.params.mkString("[", ",", "]")}")
    skinnyCRUDMapper.updateById(id).withAttributes(parameters)
  }

  def updateResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    skinnyCRUDMapper.findById(id).map { m =>
      if (updateForm.validate()) {
        if (!updateFormStrongParameters.isEmpty) {
          val parameters = params.permit(updateFormStrongParameters: _*)
          doUpdate(id, parameters)
        } else {
          throw new StrongParametersException(
            "'updateFormStrongParameters' or 'updateFormTypedStrongParameters' must be defined.")
        }
        status = 200
        format match {
          case Format.HTML =>
            set(resourceName, skinnyCRUDMapper.findById(id).getOrElse(haltWithBody(404)))
            render(s"/${resourcesName}/show")
          case _ =>
        }
      } else {
        render(s"/${resourcesName}/edit")
      }
    } getOrElse haltWithBody(404)
  }

  def destroyResource(id: Long)(implicit format: Format = Format.HTML): Any = withFormat(format) {
    skinnyCRUDMapper.findById(id).map { m =>
      skinnyCRUDMapper.deleteById(id)
      status = 200
    } getOrElse haltWithBody(404)
  }

  // ------------------
  // Routing
  // ------------------

  private[this] implicit val skinnyController: SkinnyController = this

  before() {
    set("resourceName", resourceName)
    set("resourcesName", resourcesName)
  }

  // --------------
  // create

  // should be defined in front of 'show
  get(s"${basePath}/new")(newResource).as('new)

  post(s"${basePath}/?")(createResource).as('create)

  // --------------
  // show

  get(s"${basePath}/?")(showResources()).as('index)

  get(s"${basePath}.:ext") {
    (for {
      ext <- params.get("ext")
    } yield {
      ext match {
        case "json" => showResources()(Format.JSON)
        case "xml" => showResources()(Format.XML)
        case _ => haltWithBody(404)
      }
    }) getOrElse haltWithBody(404)
  }.as('index)

  get(s"${basePath}/:id") {
    if (params.getAs[String]("id").exists(_ == "new")) {
      newResource()
    } else {
      params.getAs[Long]("id").map { id => showResource(id) } getOrElse haltWithBody(404)
    }
  }.as('show)

  get(s"${basePath}/:id.:ext") {
    (for {
      id <- params.getAs[Long]("id")
      ext <- params.get("ext")
    } yield {
      ext match {
        case "json" => showResource(id)(Format.JSON)
        case "xml" => showResource(id)(Format.XML)
        case _ => haltWithBody(404)
      }
    }) getOrElse haltWithBody(404)
  }.as('show)

  // --------------
  // update

  get(s"${basePath}/:id/edit") {
    params.getAs[Long]("id").map(id => editResource(id)) getOrElse haltWithBody(404)
  }.as('edit)

  post(s"${basePath}/:id") {
    params.getAs[Long]("id").map(id => updateResource(id)) getOrElse haltWithBody(404)
  }.as('update)

  put(s"${basePath}/:id") {
    params.getAs[Long]("id").map(id => updateResource(id)) getOrElse haltWithBody(404)
  }.as('update)

  patch(s"${basePath}/:id") {
    params.getAs[Long]("id").map(id => updateResource(id)) getOrElse haltWithBody(404)
  }.as('update)

  // --------------
  // delete

  delete(s"${basePath}/:id") {
    params.getAs[Long]("id").map(id => destroyResource(id)) getOrElse haltWithBody(404)
  }.as('destroy)

}
