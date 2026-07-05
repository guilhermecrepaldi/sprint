from pydantic import BaseModel


class IdentifyTopicIn(BaseModel):
    image_base64: str


class IdentifyTopicOut(BaseModel):
    skill_tag: str
    display_name: str
    confidence: float
